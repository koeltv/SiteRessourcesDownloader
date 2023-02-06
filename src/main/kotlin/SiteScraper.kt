import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.net.URL
import java.time.Duration

sealed class SiteScraper {
    companion object {
        internal val successFile = File("./save/success.txt")
        internal val failureFile = File("./save/failure.txt")
    }

    private val driverOptions = FirefoxOptions()

    open val linksRegex = Regex("(<a .*href=\"([^ ]+)\")|(src=\"([^ ]+\\..+)\")")
    abstract val filteredLinksRegex: Regex?
    abstract val fileRegex: Regex

    open val consumedLinks = mutableSetOf<String>()

    open val linksToConsume = mutableSetOf<String>()

    init {
        WebDriverManager.firefoxdriver().setup()
        val browserPath = WebDriverManager.firefoxdriver().browserPath
            .orElseThrow { Exception("Firefox not found") }
            .also { println("Firefox found at $it") }

        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_BINARY, browserPath.toString())
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null")

        // To prevent download dialog
        driverOptions.profile.setPreference("browser.download.folderList", 2)
        driverOptions.profile.setPreference("browser.download.manager.showWhenStarting", false)
        driverOptions.profile.setPreference("browser.download.dir", UserInterface.downloadFolder)

        load()
    }

    /**
     * Load all unexplored URL from failureFile and add the already explored URL to consumedLinks
     *
     */
    private fun load() {
        File("./save").mkdir()

        successFile.createNewFile()
        successFile.useLines { lineSequence ->
            lineSequence.forEach { consumedLinks.add(it) }
        }

        //recuperate all failed links and add them to the process list
        failureFile.createNewFile()
        failureFile.readLines().forEach { linksToConsume.add(it) }
        failureFile.delete()
        failureFile.createNewFile()
    }

    /**
     * Check if the scraper can handle the given URL
     *
     * @param url
     * @return
     */
    abstract fun canHandle(url: String): Boolean

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
                registerLink(link)

                val sourceCode = scrapeWebsite(link)
                linksRegex.findAll(sourceCode)
                    .map { match ->
                        match.destructured.component2().ifBlank { match.destructured.component4() }
                    }.filter {
                        it !in consumedLinks && !it.matches(filteredLinksRegex ?: Regex(".*")) && it.contains(baseURL)
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
        return waitUntilOrTimeout(30000) {
            ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(By.className("downloadIconCancel")))
        }.also { Thread.sleep(3000) }
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
        val driver = FirefoxDriver(driverOptions)
        return block.invoke(driver).also { driver.quit() }
    }

    /**
     * Save all links to be consumed in the failureFile
     *
     */
    fun saveState() {
        linksToConsume.forEach { registerLink(it, true) }
    }

    /**
     * Register a link.
     * A link is registered as a success when its content was successfully used, that means either all links were exported from it
     * or the file it contained was successfully downloaded.
     *
     * @param url
     * @param failed true if the link is a failure, false otherwise
     */
    fun registerLink(url: String, failed: Boolean = false) {
        val text = "$url\r\n"
        if (failed) {
            failureFile.appendText(text)
        } else {
            successFile.appendText(text)
        }
    }
}