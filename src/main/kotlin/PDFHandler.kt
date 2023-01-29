import java.awt.Color
import java.awt.Point
import java.io.File

object PDFHandler {
    private const val DELAY_BETWEEN_USER_ACTION = 2000L

    private val screenPoints = mutableMapOf<String, Point>()

    /**
     * Export file to PDF using print menu
     */
    fun export() {
        Thread.sleep(5000)

        //Select print button
        val printButtonPosition =
            screenPoints.getOrPut("printButtonPosition") { UserInterface.waitAndReturnMousePosition() }
        UserInterface.mouseMove(printButtonPosition)
        UserInterface.click()

        UserInterface.mouseMove(printButtonPosition)
        UserInterface.click()

        //Wait for print menu to load
        waitForPrintMenu(printButtonPosition)
//        Thread.sleep(15000) //https://padlet.com/cecile_laloux/HARCELEMENT_ARCIS_6_JANV/wish/1059086229

        //Select destination
        val destinationButtonPosition =
            screenPoints.getOrPut("destinationButtonPosition") { UserInterface.waitAndReturnMousePosition() }
        UserInterface.mouseMove(destinationButtonPosition)
        UserInterface.click()

        //Select output
        Thread.sleep(1000)
        val toPDFButtonPosition =
            screenPoints.getOrPut("toPDFButtonPosition") { UserInterface.waitAndReturnMousePosition() }
        UserInterface.mouseMove(toPDFButtonPosition)
        UserInterface.click()

        //Confirm
        Thread.sleep(DELAY_BETWEEN_USER_ACTION)
        UserInterface.confirm()

        Thread.sleep(DELAY_BETWEEN_USER_ACTION)
        UserInterface.nameFile()

        //Validate filename and save
        Thread.sleep(DELAY_BETWEEN_USER_ACTION)
        UserInterface.confirm()

        waitForDownloadViaPrint()
//        Thread.sleep(25000)
    }

    /**
     * Wait for print menu
     *
     * @param printButtonPosition
     */
    private fun waitForPrintMenu(printButtonPosition: Point) {
        Thread.sleep(250)
        val startingPoint = System.currentTimeMillis()
        val previousState = UserInterface.getPixelColor(printButtonPosition)
        var newState: Color
        do {
            Thread.sleep(500)
            newState = UserInterface.getPixelColor(printButtonPosition)
        } while (newState == previousState && System.currentTimeMillis() - startingPoint < 30000)
        Thread.sleep(1000)
    }

    /**
     * Wait for a download via the print menu to complete
     *
     */
    private fun waitForDownloadViaPrint() {
        val downloadedFile = File("${UserInterface.downloadFolder}/file${UserInterface.fileNumber - 1}.pdf")
        var fileSize: Long
        do {
            fileSize = downloadedFile.length()
            Thread.sleep(1000)
        } while (downloadedFile.length() != fileSize)
        Thread.sleep(1000)
    }
}