import java.io._
import java.net.URL
import java.util.Properties
import javax.sound.sampled._
import javazoom.spi.PropertiesContainer
import scala.actors.Actor


class OggPlayer(filename: String) extends Actor {
  def act() {
    play
  }

  def play() = {
    println(filename)
    val file = new File(filename)
    // Get AudioInputStream from given file.	
    val in = AudioSystem.getAudioInputStream(file)
    var din: AudioInputStream = null
    if (in != null) {
      val baseFormat = in.getFormat()
      val decodedFormat = new AudioFormat(
              AudioFormat.Encoding.PCM_SIGNED,
              baseFormat.getSampleRate(),
              16,
              baseFormat.getChannels(),
              baseFormat.getChannels() * 2,
              baseFormat.getSampleRate(),
              false)
       // Get AudioInputStream that will be decoded by underlying VorbisSPI
      din = AudioSystem.getAudioInputStream(decodedFormat, in)
      // Play now !
      rawplay(decodedFormat, din)
      in.close()		
    }
  }

  def rawplay(targetFormat: AudioFormat, din: AudioInputStream) = {
    val data = new Array[Byte](4096)
    val line = getLine(targetFormat)
    if (line != null)
    {
      // Start
      line.start()
      var nBytesRead = 0
      var nBytesWritten = 0
      while (nBytesRead != -1)
      {
        nBytesRead = din.read(data, 0, data.length)

        if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead)
      }
      // Stop
      //line.drain()
      line.stop()
      line.close()
      din.close()
    }		
  }

  def getLine(audioFormat: AudioFormat) = {
    val res = AudioSystem.getSourceDataLine(audioFormat)
    res.open(audioFormat)
    res
  }
}
