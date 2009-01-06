package server

import shared._

import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

class Projectile(server: Server, val tank: Tank) extends GameObject(server) with shared.Projectile {
  var collidedWith: GameObject = _
    
  body.setMassFromShapes
  
  override def shapes: List[ShapeDef] = {
    val sDef = new CircleDef
    sDef.radius = radius
    sDef.restitution = 0f
    sDef.density = 1f
    List(sDef)
  }

  override def bodyDef = {
    val bDef = new BodyDef
    bDef.isBullet = true
    bDef
  }

  def update(delta : Int) {
    if (null != collidedWith) {
      explode(collidedWith)
    }
    body.applyForce(new Vec2(0, Projectile.antiGravity * body.getMass), body.getPosition)
    if (y > Main.GAME_HEIGHT || destroy) {
      server.removeProjectile(this)
    }
    else if (y + radius > server.ground.heightAt(x) || 
             x < 0 || 
             x > Main.GAME_WIDTH) {
      collide(server.ground, null)
    }
    
    if (!round) {
      body.setXForm(body.getPosition, (Math.atan2(body.getLinearVelocity.x, -body.getLinearVelocity.y)).toFloat)
    }
  }
  
  override def collide(obj: GameObject, contact: ContactPoint) {
    if (!obj.isInstanceOf[Projectile] && !obj.isInstanceOf[Explosion]) {
      collidedWith = obj
    }
  }
  
  def explode(obj: GameObject) {
    if (destroy) {
      return
    }
    
    destroy = true

    server.addExplosion(x, y, explosionRadius, this, explosionDamageFactor)
    server.ground.deform(x, y, explosionRadius)
    
    if (obj.isInstanceOf[Tank]) {
      val hitTank = obj.asInstanceOf[Tank]
      
      hitTank.damage(damage, this)
      if (tank != null && tank.player != null) {
        println(tank.player.name + " hit " + hitTank.player.name + " directly with a " + this.getClass.getName + " for " + damage + " damage.")
      }
    }
  }
 
  def onRemove {
    server.removeBody(body)
  }

  def serialise = {
    Operations.toByteArray((
      id,
      x,
      y,
      body.getLinearVelocity.x,
      body.getLinearVelocity.y,
      body.getAngle,
      body.getAngularVelocity,
      projectileType.id.toByte
    ))
  }
}
