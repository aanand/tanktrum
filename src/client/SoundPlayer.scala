package client

import shared.Resource
import java.io._
import java.net.URL
import java.util.Properties
import javax.sound.sampled._
import javazoom.spi.PropertiesContainer
import scala.actors.Actor
import scala.collection.mutable.HashMap

case class PlaySound(s: String) {}

object SoundPlayer {
  val files  = List(
    "death.wav",
    "explosion.ground1.wav",
    "explosion.tank1.wav",
    "fire1.wav",
    "fire2.wav",
    "fire3.wav",
    "fire4.wav",
    "reload.wav",
    "thruster.wav"
  )

  val dir = "media/sounds"
  val sounds = new HashMap[String, (AudioFormat, Array[Byte])]()
  
  for (file <- files) {
    try {
      sounds(file) = decodeFile(dir + "/" + file)
    } catch {
      case e: Exception => println("Warning: Couldn't load sound: " + file)
    }
  }

  def play(filename: String): Clip = {
    play(filename, false)
  }

  def play(filename: String, loop: Boolean): Clip = {
    val (format, data) = sounds(filename)
    val clip = AudioSystem.getClip

    clip.addLineListener(new SoundListener)

    try {
      clip.open(format, data, 0, data.length)
    }
    catch {
      case e:LineUnavailableException => println("Warning: no available sound lines.")
    }
    
    if (loop) {
      clip.loop(Clip.LOOP_CONTINUOUSLY)
    } else {
      clip.start
    }

    clip
  }

  def decodeFile(fileName: String) = {
    var rawStream: AudioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(Resource.get(fileName)))
    
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
