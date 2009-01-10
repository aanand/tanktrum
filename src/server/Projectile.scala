package server

import shared._
import shared.ProjectileTypes._

import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object Projectile {
  val antiGravity = Config("physics.projectileGravity").toFloat - Config("physics.gravity").toFloat
  
  def create(server: Server, tank: Tank, projectileType: ProjectileTypes.Value): Projectile = {
    projectileType match {
      case PROJECTILE          => new Projectile(server, tank) 
      case NUKE                => new Nuke(server, tank) 
      case ROLLER              => new Roller(server, tank) 
      case MIRV                => new Mirv(server, tank) 
      case MIRV_CLUSTER        => new MirvCluster(server, tank)
      case CORBOMITE           => new Corbomite(server, tank)
      case MACHINE_GUN         => new MachineGun(server, tank) 
      case DEATHS_HEAD         => new DeathsHead(server, tank)
      case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster(server, tank)
      case MISSILE             => new Missile(server, tank)
    }
  }
}

class Projectile(server: Server, val tank: Tank) extends GameObject(server) {
  var id: Int = -1
  val projectileType = ProjectileTypes.PROJECTILE
  def name = getClass.getName.split("\\.").last
  
  def conf(property: String) = Config("projectile." + name + "." + property)

  def explosionRadius = conf("explosionRadius").toFloat
  def explosionDamageFactor = conf("damageFactor").toFloat
  def radius = conf("radius").toFloat
  def damage = conf("damage").toInt
  val reloadTime = conf("reload").toFloat

  var collidedWith: GameObject = _

  var destroy = false

  body.setMassFromShapes
  def round = Config("projectile." + name + ".round").toBoolean
  
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
        println(tank.player.name + " hit " + hitTank.player.name + " directly with a " + name + " for " + damage + " damage.")
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
      body.getAngle,
      projectileType.id.toByte
    ))
  }
}
