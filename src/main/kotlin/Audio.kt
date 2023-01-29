import javax.sound.sampled.AudioSystem

class Audio(url: String) {
    private val audioFile = javaClass.getResource(url)

    fun play() {
        Thread { run() }.start()
    }

    private fun run() {
        AudioSystem.getClip().use {
            it.open(AudioSystem.getAudioInputStream(audioFile))
            it.start()
            Thread.sleep(it.microsecondLength / 1000)
            it.stop()
        }
    }
}