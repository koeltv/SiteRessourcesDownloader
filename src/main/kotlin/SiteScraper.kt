import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.net.URL
import java.time.Duration

abstract class SiteScraper {
    companion object {
        internal val successFile = File("./save/success.txt")
        internal val failureFile = File("./save/failure.txt")
    }

    abstract val linksRegex: Regex
    abstract val filteredLinksRegex: Regex
    abstract val fileRegex: Regex

    abstract val consumedLinks: MutableSet<String>

    abstract val linksToConsume: MutableSet<String>

    init {
        WebDriverManager.firefoxdriver().setup()

        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null")

        // To prevent download dialog
        val profile = FirefoxProfile()
        profile.setPreference("browser.download.folderList", 2) // custom location
        profile.setPreference("browser.download.manager.showWhenStarting", false)
        profile.setPreference("browser.download.dir", "./download")
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv")
    }

    /**
     * Load all unexplored URL from failureFile
     *
     */
    abstract fun load()

    /**
     * Register a link as a success.
     * A link is registered as a success when its content was successfully used, that means either all links were exported from it
     * or the file it contained was successfully downloaded.
     *
     * @param url
     */
    fun registerSuccess(url: String) {
        successFile.appendText(url + "\n")
    }

    /**
     * Register a link as a failure.
     * A link is registered as a failure when the link(s) it contains where not exploited or the file it contains wasn't downloaded correctly.
     *
     * @param url
     */
    fun registerFailure(url: String) {
        failureFile.appendText(url + "\n")
    }

    /**
     * Count downloaded files (all files in the Download folder)
     *
     * @return
     */
    fun countDownloadedFiles(): Int {
        return File(UserInterface.downloadFolder).listFiles()?.size?.let { it - 1 } ?: 0
    }

    /**
     * Download all accessibles files
     *
     * @param url the url to search files in
     */
    open fun downloadFiles(url: String) {
        val baseURL = URL(url).let { it.protocol + "://" + it.host }
        consumedLinks.add(baseURL)
        linksToConsume.add(url)

        while (linksToConsume.isNotEmpty()) {
            val link = linksToConsume.random()
            linksToConsume.remove(link)

            if (link.matches(fileRegex)) {
                downloadFile(link)
            } else {
                consumedLinks.add(link)
                successFile.appendText(link + "\n")

                val sourceCode = scrapeWebsite(link)
                linksRegex.findAll(sourceCode)
                    .map { match ->
                        match.destructured.component2().ifBlank { match.destructured.component4() }
                    }.filter {
                        it !in consumedLinks && !it.matches(filteredLinksRegex) && it.contains(baseURL)
                    }.forEach {
                        linksToConsume.add(it)
                    }
            }
        }
    }

    /**
     * Download file
     *
     * @param url the url where to find the file
     */
    abstract fun downloadFile(url: String)

    /**
     * Scrape website source code
     *
     * @param url
     * @return the source code of the page
     */
    private fun scrapeWebsite(url: String): String {
        return withDriver { driver ->
            driver.get(url)
            Thread.sleep(5000) //TODO Make configurable
            driver.pageSource
        }
    }

    /**
     * Wait until the given condition is fulfilled or timeout
     *
     * @param R the result type
     * @param timeout
     * @param predicate the condition to fulfill
     * @return true if the timeout was reached, false otherwise
     */
    fun <R> WebDriver.waitUntilOrTimeout(timeout: Long, predicate: (WebDriver) -> R): Boolean {
        return try {
            WebDriverWait(this, Duration.ofMillis(timeout)).until(predicate)
            false
        } catch (e: TimeoutException) {
            true
        }
    }

    /**
     * Wait for a download to complete
     *
     * @return true if the timeout was reached, false otherwise
     */
    fun WebDriver.waitForDownload(): Boolean {
        get("about:downloads")
        return waitUntilOrTimeout(20000) {
            ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(By.className("downloadIconCancel")))
        }.also { Thread.sleep(1000) }
    }

    /**
     * Wait for at least 1 element to be returned
     *
     * @param function the function returning the element(s)
     * @return the element(s) found
     */
    fun waitForElements(function: () -> List<WebElement>?): List<WebElement> {
        var elements: List<WebElement>?
        do {
            elements = function()
        } while (elements.isNullOrEmpty())
        return elements
    }

    /**
     * Instantiate a driver and apply the given block using it.
     *
     * @param R the result of the block to be executed
     * @param block the block to execute
     * @return
     */
    fun <R> withDriver(block: (WebDriver) -> R): R {
        val driver = FirefoxDriver()
        return block.invoke(driver).also { driver.quit() }
    }

    /**
     * Save all links to be consumed in the failureFile
     *
     */
    fun saveState() {
        linksToConsume.forEach { failureFile.appendText(it + "\n") }
    }
}