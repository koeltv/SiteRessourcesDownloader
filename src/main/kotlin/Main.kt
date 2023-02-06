import java.util.*
import kotlin.reflect.full.createInstance

fun main() {
    val scanner = Scanner(System.`in`)

    println("Entrer l'URL à scanner (ex: https://website.com/custom_path)")
    print("> ")

    val url = scanner.nextLine()

    SiteScraper::class.sealedSubclasses
        .map { it.createInstance() }
        .find { it.canHandle(url) }
        ?.let {
            Runtime.getRuntime().addShutdownHook(Thread { it.saveState() })
            it.downloadFiles(url)
        } ?: println("URL non supportée")
}