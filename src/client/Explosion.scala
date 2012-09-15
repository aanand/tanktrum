package client

import GL._

import shared.Config

import sbinary.Instances._
import sbinary.Operations

import org.newdawn.slick._

object Explosion {
  val images = new scala.collection.mutable.HashMap[Int, Image]
  val frames = 6

  def frame(frameNum: Int) = {
    images.getOrElseUpdate(frameNum, new Image("media/explosions/boom" + frameNum + ".png"))
  }
}

class Explosion (client: Client) extends GameObject {
  val animationLifetime = Config("explosion.animationLifetime").toFloat
  var animationTime = animationLifetime

  var radius: Float = _
  var isTankExplosion = false
  
  def update(delta: Int) {
    animationTime -= delta/1000f

    if (animationTime < 0) {
      client.removeExplosion(this)
    }
  }
  
  def render(g: Graphics) {
    val frameNum = Math.min(6, (Explosion.frames * (1-animationTime/animationLifetime)).toInt + 1)

    val image = Explosion.frame(frameNum)
    texture(image.getTexture.getTextureID) {
      color(1f, 1, 1f, 1f)
      image.draw(x - radius, y - radius, radius*2, radius*2)
    }
  }

  def loadFrom(data: Array[Byte]) = {
    val (newX, newY, newRadius, newIsTankExplosion) = Operations.fromByteArray[(Float, Float, Float, Boolean)](data)
    x = newX
    y = newY
    radius = newRadius
    isTankExplosion = newIsTankExplosion
  }

  def playSound {
    val sound = if (isTankExplosion) "explosion.tank1.wav" else "explosion.ground1.wav"
    SoundPlayer.play(sound)
  }
}


