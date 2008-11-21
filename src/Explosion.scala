import org.newdawn.slick._
import sbinary.Instances._
import sbinary.Operations

import scala.collection.mutable.HashMap

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

class Explosion (var x: Float, var y: Float, var radius: Float, session: Session, projectile: Projectile, damageFactor: Float) extends GameObject(session) {
  val animationLifetime = Config("explosion.animationLifetime").toFloat
  val damageLifetime = Config("explosion.damageLifetime").toFloat
  var animationTime = animationLifetime
  var damageTime = damageLifetime

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
    damageTime -= delta/1000f
    animationTime -= delta/1000f
    if (animationTime < 0) {
      session.removeExplosion(this)
    }
  }

  def render(g: Graphics) {
    g.setColor(new Color(0.5f, 0.5f, 0.8f, animationTime/animationLifetime))
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
    if (other.isInstanceOf[ServerTank]) {
      val tank = other.asInstanceOf[ServerTank]
      var damage = -contact.separation
      damage *= (damageTime/damageLifetime)
      damage *= damageFactor
      damage *= Main.GAME_WINDOW_RATIO
      
      if (damage > 0) {
        if (!tanksHit.contains(tank)) {
          println(tank.player.name + " in " + projectile.getClass.getName + " explosion for " + damage + " damage.")
          tanksHit(tank) = damage
          tank.damage(damage, this.projectile)
        }
        else {
          val oldDamage = tanksHit(tank)
          if (damage > oldDamage) {
            println(tank.player.name + " in " + projectile.getClass.getName + " explosion for a further " + (damage-oldDamage) + " damage.")
            tanksHit(tank) = damage
            tank.damage(damage-oldDamage, this.projectile)
          }
        }
      }
    }
  }
}
