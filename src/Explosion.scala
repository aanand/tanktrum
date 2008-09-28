import org.newdawn.slick._
import sbinary.Instances._
import sbinary.Operations

import scala.collection.mutable.HashMap

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

class Explosion (var x: Float, var y: Float, var radius: Float, session: Session, projectile: Projectile, damageFactor: Float) extends GameObject(session) {
  val lifetime = Config("explosion.lifetime").toFloat
  var timeToDie = lifetime

  val sound = "explosion1.wav"
  
  var tanksHit = new HashMap[Tank, float]

  body.setXForm(new Vec2(x, y), 0f)
  
  override def shapes = {
    val expShape = new CircleDef
    expShape.radius = radius
    expShape.isSensor = true
    List(expShape)
  }

  if (session.isInstanceOf[Client]) {
    SoundPlayer ! PlaySound(sound)
  }
    
  def update(delta: Int) {
    timeToDie -= delta/1000f
    if (timeToDie < 0) {
      session.removeExplosion(this)
    }
  }

  def render(g: Graphics) {
    g.setColor(new Color(0.5f, 0.5f, 0.8f, timeToDie/lifetime))
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }

  def serialise = {
    Operations.toByteArray((
      x,
      y,
      radius
    ))
  }

  def loadFrom(data: Array[Byte]) = {
    val (newX, newY, newRadius) = Operations.fromByteArray[(Float, Float, Float)](data)
    x = newX
    y = newY
    radius = newRadius
  }

  override def collide(other: GameObject, contact: ContactPoint) {
    if (other.isInstanceOf[Tank]) {
      val tank = other.asInstanceOf[Tank]
      var damage = -contact.separation
      damage *= (timeToDie/lifetime)
      damage *= damageFactor

      if (!tanksHit.contains(tank)) {
        println(tank.player.name + " in explosion for " + damage + " damage.")
        tanksHit(tank) = damage
        tank.damage(damage, this.projectile)
      }
      else {
        val oldDamage = tanksHit(tank)
        if (damage > oldDamage) {
          println(tank.player.name + " in explosion for a further " + (damage-oldDamage) + " damage.")
          tanksHit(tank) = damage
          tank.damage(damage-oldDamage, this.projectile)
        }
      }
    }
  }
}
