package client;

import java.io._
import java.net.URL
import java.util.Properties
import javax.sound.sampled._
import javazoom.spi.PropertiesContainer
import scala.actors.Actor
import scala.collection.mutable.HashMap

case class PlaySound(s: String) {}

object SoundPlayer extends Actor {
  var files = List(
    "explosion.ogg",
    "explosion1.wav"
  )
  
  val sounds = new HashMap[String, (AudioFormat, Array[Byte])]()
  
  for (file <- files) {
    sounds(file) = decodeFile("media/sounds/" + file)
  }

  def act {
    while (true) {
      receive {
        case PlaySound(sound) => play(sound)
      }
    }
  }
  
  def play(filename: String) = {
    println("Playing: " + filename)

    val (format, data) = sounds(filename)
    val clip = AudioSystem.getClip

    clip.addLineListener(new SoundListener)

    try {
      clip.open(format, data, 0, data.length)
    }
    catch {
      case e:LineUnavailableException => println("Warning: no available sound lines.")
    }

    clip.start
  }

  def decodeFile(fileName: String) = {
    var rawStream: AudioInputStream = null
    if (new File(fileName).exists) {
      rawStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(fileName)))
    }
    else {
      rawStream = AudioSystem.getAudioInputStream(new BufferedInputStream(getClass.getResourceAsStream(fileName)))
    }
    
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

class SoundListener extends LineListener {
  def update(e: LineEvent) {
    if (e.getType == LineEvent.Type.STOP) {
      e.getLine.close
    }
  }
}

class SoundFileFilter extends FilenameFilter {
  override def accept(dir: File, fileName: String) = {
    fileName.matches(".*.ogg") || fileName.matches(".*.wav")
  }
}
