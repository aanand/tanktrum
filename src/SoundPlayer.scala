import java.io._
import java.net.URL
import java.util.Properties
import javax.sound.sampled._
import javazoom.spi.PropertiesContainer
import scala.actors.Actor
import scala.collection.mutable.HashMap

object DeathTankSounds {
  val files = new File("media/").list(new OggFileFilter)
  val mixer = AudioSystem.getMixer(null)
  val sounds = new HashMap[String,Array[Byte]]()
  for (file <- files) {
    sounds(file) = readFile("media/" + file)
  }
  val formatStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(sounds.values.next))
  val baseFormat = formatStream.getFormat()
  formatStream.close

  val format = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        baseFormat.getSampleRate(),
        16,
        baseFormat.getChannels(),
        baseFormat.getChannels() * 2,
        baseFormat.getSampleRate(),
        false) 
  
  def play(filename: String) = {
    println(filename)
    // Get AudioInputStream from given file.
    val in = AudioSystem.getAudioInputStream(format, 
               AudioSystem.getAudioInputStream(
               new ByteArrayInputStream(sounds(filename))))
    val data = new Array[Byte](4096)
    val line = getLine(format)
    if (line != null)
    {
      // Start
      line.start()
      var nBytesRead = 0
      var nBytesWritten = 0
      while (nBytesRead != -1) {
        nBytesRead = in.read(data, 0, data.length)

        if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead)
      }
      // Stop
      line.drain()
      line.stop()
      line.close()
    }
    in.close()
  }

  def getLine(audioFormat: AudioFormat) = {
    val newLine = mixer.getLine(mixer.getSourceLineInfo()(0)).asInstanceOf[SourceDataLine]
    newLine.open(audioFormat)
    newLine
  }

  def readFile(fileName: String) = {
    val byteBuf = new Array[Byte](1024)
    val outStream = new ByteArrayOutputStream(4096)
    val inStream = new FileInputStream(fileName)
    var read = 0
    while (read >= 0) {
      outStream.write(byteBuf, 0, read)
      read = inStream.read(byteBuf)
    }
    outStream.toByteArray
  }
}

class SoundPlayer(sound: String) extends Actor {
  def act() {
    DeathTankSounds.play(sound)
  }
}

class OggFileFilter extends FilenameFilter {
  override def accept(dir: File, fileName: String) = {
    fileName.matches(".*.ogg")
  }
}
