package client

import shared.Config

import sbinary.Instances._
import sbinary.Operations

import org.newdawn.slick._

class Explosion (client: Client) extends GameObject {
  val animationLifetime = Config("explosion.animationLifetime").toFloat
  var animationTime = animationLifetime
  
  val sound = "explosion1.wav"
  SoundPlayer ! PlaySound(sound)

  var radius: Float = _
  
  def update(delta: Int) {
    animationTime -= delta/1000f
    if (animationTime < 0) {
      client.removeExplosion(this)
    }
  }
  
  def render(g: Graphics) {
    g.setColor(new Color(0.5f, 0.5f, 0.8f, animationTime/animationLifetime))
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }

  def loadFrom(data: Array[Byte]) = {
    val (newX, newY, newRadius) = Operations.fromByteArray[(Float, Float, Float)](data)
    x = newX
    y = newY
    radius = newRadius
  }
}


