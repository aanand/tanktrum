package client

import GL._

import shared.Config

import sbinary.Instances._
import sbinary.Operations

import org.newdawn.slick._

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
    color(0.5f, 0.5f, 0.8f, animationTime/animationLifetime)
    oval(x - radius, y - radius, radius*2, radius*2)
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
    SoundPlayer ! PlaySound(sound)
  }
}


