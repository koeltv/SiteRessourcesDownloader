import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.support.ui.ExpectedConditions
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class PadletScraper : SiteScraper() {
    override val linksRegex = Regex("(<a .*href=\"([^ ]+)\")|(src=\"([^ ]+\\.pdf)\")")
    override val filteredLinksRegex = Regex(".+/auth/signup.*")
    override val fileRegex = Regex(".+/wish/\\d+")

    override val linksToConsume = mutableSetOf<String>()
    override val consumedLinks = mutableSetOf(
        "https://padlet.com/auth/signup",
        "https://padlet.com/auth/login",
        "https://padlet.com/auth/org_login_redirect",
        "https://padlet.com/about/accessibility",
        "https://padlet.com/contact-us",
    )

    init {
        load()
    }

    override fun load() {
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

    @Suppress("unused")
    fun downloadPage(website: URL, link: String) {
        // Download the file
        val fileUrl = URL(website, link)
        val fileIn = fileUrl.openStream()

        val outputPath =
            "./download" + link.removePrefix(website.path).removePrefix(website.protocol + "://" + website.host)
                .replace(Regex("[?=]|(%[A-Z0-9]*)"), "")
        outputPath.split("/").fold("") { relativePath, directory ->
            val path = "$relativePath/$directory"
            File(path).mkdir()
            path
        }

        val fileOut = FileOutputStream(outputPath)
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (fileIn.read(buffer).also { bytesRead = it } != -1) {
            fileOut.write(buffer, 0, bytesRead)
        }
        fileIn.close()
        fileOut.close()
    }

    /**
     * Download file
     *
     * @param url the url were to find the file
     */
    override fun downloadFile(url: String) {
        withDriver { driver ->
            driver.get(url)
            val didTimeout = driver.waitUntilOrTimeout(5000) {
                ExpectedConditions.visibilityOf(driver.findElement(By.xpath("//iframe[@class='wb-embed-content']")))
            }
            //If the iframe wasn't found, exit
            if (didTimeout) {
                failureFile.appendText(url + "\n")
                return@withDriver
            }

            //Click on the 3 dots
            waitForElements {
                driver.findElements(By.xpath("//i[text()='dot_3_horizontal' and @style='font-size: 24px; max-width: 24px; width: 24px; height: 24px; overflow: visible;']"))
            }.last().click()

            val nbOfDownloadedFiles = countDownloadedFiles()
            try {
                val mainTab = driver.windowHandle

                driver.findElement(By.xpath("//i[text()='attachment_download']")).click()
                //Wait for the download to start
                Thread.sleep(2000)

                //If the download button exist but no files are being downloaded
                if (countDownloadedFiles() <= nbOfDownloadedFiles) {
                    driver.switchTo().window(mainTab)
                    PDFHandler.export()
                } else {
                    driver.waitForDownload()
                }
            } catch (e: NoSuchElementException) {
                PDFHandler.export()
            }

            if (countDownloadedFiles() > nbOfDownloadedFiles) {
                registerSuccess(url)
            } else {
                registerFailure(url)
            }
        }
    }
}