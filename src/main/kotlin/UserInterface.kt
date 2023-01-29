import java.awt.Color
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.File

object UserInterface {
    private val robot = Robot()

    /**
     * Download folder of the user calling this program
     */
    val downloadFolder = System.getProperty("user.dir").let { path ->
        val userPath = Regex("(.+\\\\Users\\\\\\w+\\\\)").find(path)!!.destructured.component1()
        userPath + "Downloads"
    }

    internal var fileNumber = 0

    init {
        File(downloadFolder).listFiles()?.forEach { file ->
            Regex("file(\\d+)").find(file.name)?.destructured?.component1()?.let { s ->
                val number = s.toInt()
                if (number >= fileNumber) fileNumber = number + 1
            }
        }

    }

    /**
     * Simulate a mouse click
     *
     * @param inputEvent
     */
    fun click(inputEvent: Int = MouseEvent.BUTTON1_DOWN_MASK) {
        robot.mousePress(inputEvent)
        robot.mouseRelease(inputEvent)
    }

    /**
     * Simulate a key press
     *
     * @param keyEvent
     */
    private fun pressKeyOnce(keyEvent: Int) {
        robot.keyPress(keyEvent)
        robot.keyRelease(keyEvent)
    }

    /**
     * Name a file
     * All files are named following this pattern: "`fileNUMBER`", NUMBER being an incremental index starting from 0
     *
     */
    fun nameFile() {
        pressKeyOnce(KeyEvent.VK_F)
        pressKeyOnce(KeyEvent.VK_I)
        pressKeyOnce(KeyEvent.VK_L)
        pressKeyOnce(KeyEvent.VK_E)

        for (digit in fileNumber++.toString()) {
            pressNumberKeyOnce(digit.digitToInt())
        }
    }

    /**
     * Press the corresponding number keypad key once
     *
     * @param digit
     */
    private fun pressNumberKeyOnce(digit: Int) {
        pressKeyOnce(when(digit) {
            0 -> KeyEvent.VK_NUMPAD0
            1 -> KeyEvent.VK_NUMPAD1
            2 -> KeyEvent.VK_NUMPAD2
            3 -> KeyEvent.VK_NUMPAD3
            4 -> KeyEvent.VK_NUMPAD4
            5 -> KeyEvent.VK_NUMPAD5
            6 -> KeyEvent.VK_NUMPAD6
            7 -> KeyEvent.VK_NUMPAD7
            8 -> KeyEvent.VK_NUMPAD8
            9 -> KeyEvent.VK_NUMPAD9
            else -> throw IllegalArgumentException("Not a digit !")
        })
    }

    /**
     * Move the mouse to the given coordinates
     *
     * @param point
     */
    fun mouseMove(point: Point) {
        robot.mouseMove(point.x, point.y)
    }

    /**
     * Get pixel color at given point
     *
     * @param point
     * @return
     */
    fun getPixelColor(point: Point): Color {
        return robot.getPixelColor(point.x, point.y)
    }

    /**
     * Alert the user, wait and return mouse position
     * This will play a sound to the user and wait 5s for him to place his mouse in the wanted spot
     *
     * @return
     */
    fun waitAndReturnMousePosition(): Point {
        attention()
        Thread.sleep(5000)
        return MouseInfo.getPointerInfo().location
    }

    /**
     * Confirm with the `ENTER` key
     *
     */
    fun confirm() {
        pressKeyOnce(KeyEvent.VK_ENTER)
    }

    /**
     * Play a given sound to attract the user attention
     *
     */
    private fun attention() {
        Audio("clinking_teaspoon.wav").play()
    }
}