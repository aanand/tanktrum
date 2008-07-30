import java.io._
import java.net.URL
import java.util.Properties
import javax.sound.sampled._
import javazoom.spi.PropertiesContainer
import scala.actors.Actor
import scala.collection.mutable.HashMap

object DeathTankSounds {
  val files = new File("media/sounds/").list(new SoundFileFilter)
  val mixer = AudioSystem.getMixer(null)
  val sounds = new HashMap[String, (AudioFormat, Array[Byte])]()
  
  for (file <- files) {
    sounds(file) = readFile("media/sounds/" + file)
  }
  
  def play(filename: String) = {
    println(filename)

    val (format, data) = sounds(filename)
    
    val line = getLine(format)

    if (line != null) {
      line.start
      line.write(data, 0, data.length)
      line.drain
      line.stop
      line.close
    }
  }

  def getLine(audioFormat: AudioFormat) = {
    val newLine = mixer.getLine(mixer.getSourceLineInfo()(0)).asInstanceOf[SourceDataLine]
    newLine.open(audioFormat)
    newLine
  }

  def readFile(fileName: String) = {
    val rawStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(fileName)))
    
    val baseFormat = rawStream.getFormat()

    val format = new AudioFormat(
          AudioFormat.Encoding.PCM_SIGNED,
          baseFormat.getSampleRate(),
          16,
          baseFormat.getChannels(),
          baseFormat.getChannels() * 2,
          baseFormat.getSampleRate(),
          false) 
    
    val byteBuf = new Array[Byte](4096)
    val outStream = new ByteArrayOutputStream(4096)
    val inStream = AudioSystem.getAudioInputStream(format, rawStream)
    var read = 0
    while (read >= 0) {
      outStream.write(byteBuf, 0, read)
      read = inStream.read(byteBuf)
    }
    
    (format, outStream.toByteArray)
  }
}

class SoundPlayer(sound: String) extends Actor {
  def act() {
    DeathTankSounds.play(sound)
  }
}

class SoundFileFilter extends FilenameFilter {
  override def accept(dir: File, fileName: String) = {
    fileName.matches(".*.ogg") || fileName.matches(".*.wav")
  }
}
