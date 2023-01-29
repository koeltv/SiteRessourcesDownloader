import java.util.*

fun main() {
    val scanner = Scanner(System.`in`)

    println("Entrer l'URL du Padlet (ex: https://padlet.com/jean_peuplu)")
    print("> ")

    val url = scanner.nextLine()

    val padletScraper = PadletScraper()
    Runtime.getRuntime().addShutdownHook(Thread { padletScraper.saveState() })
    padletScraper.downloadFiles(url)
}