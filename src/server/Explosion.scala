package server

import shared._

import sbinary.Instances._
import sbinary.Operations


import scala.collection.mutable.HashMap

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

class Explosion (x: Float, y: Float, var radius: Float, server: Server, projectile: Projectile, damageFactor: Float) extends GameObject(server) {
  val damageLifetime = Config("explosion.damageLifetime").toFloat
  var damageTime = damageLifetime

  var tanksHit = new HashMap[Tank, float]

  body.setXForm(new Vec2(x, y), 0f)
  
  override def shapes = {
    val expShape = new CircleDef
    expShape.radius = radius
    expShape.isSensor = true
    List(expShape)
  }

  def update(delta: Int) {
    damageTime -= delta/1000f
    if (damageTime < 0) {
      server.removeExplosion(this)
    }
  }

  def serialise = {
    Operations.toByteArray((
      x,
      y,
      radius
    ))
  }

  override def collide(other: GameObject, contact: ContactPoint) {
    if (other.isInstanceOf[Tank]) {
      val tank = other.asInstanceOf[Tank]
      var damage = -contact.separation
      damage *= (damageTime/damageLifetime)
      damage *= damageFactor
      damage *= Main.GAME_WINDOW_RATIO
      
      if (damage > 0) {
        if (!tanksHit.contains(tank)) {
          println(tank.player.name + " in " + projectile.name + " explosion for " + damage + " damage.")
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
