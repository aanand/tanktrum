import sbinary.Instances._
import sbinary.Operations

import java.util.ArrayList

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object ServerTank {
  implicit def tankToServerTank(tank: Tank) = tank.asInstanceOf[ServerTank]
}

class ServerTank(server: Server, id: Byte) extends Tank(server, id) {
  val rand = new Random
  
  val SPEED = Config("tank.speed").toFloat

  val STARTING_ALTITUDE = Config("tank.startingAltitude").toFloat

  val airSpeedX       = Config("tank.air.speedX").toFloat
  val airSpeedY       = Config("tank.air.speedY").toFloat
  val airTilt         = Math.toRadians(Config("tank.air.tilt").toFloat).toFloat
  val airAngularSpeed = Math.toRadians(Config("tank.air.angularSpeed").toFloat).toFloat

  var altitudeDamageFactor = Config("tank.air.altitudeDamageFactor").toFloat
  
  val fallThreshold     = Config("tank.fall.threshold").toInt
  val fallDamageDivider = Config("tank.fall.damageDivider").toInt
  val fallImmuneTime    = Config("tank.fall.immuneTime").toInt
  var fallImmuneTimer = fallImmuneTime


  val BODY_MASS = Config("tank.bodyMass").toFloat
  val WHEEL_MASS = Config("tank.wheelMass").toFloat
  val BASE_MASS = Config("tank.baseMass").toFloat

  var thrust = 0
  var lift = 0
  var destroy = false

  var previousValues: (Float, Float, Float, Float, Float, Int, Int, Int, Int, Boolean, Int, Int, Int) = _
  
  def currentValues = {
    (x, y, angle, 
     gun.angle, gun.power, 
     gun.angleChange, gun.powerChange, 
     health, thrust, jumping, jumpFuel, 
     gun.selectedWeapon.id, 
     gun.ammo(gun.selectedWeapon))
  }

  def direction = new Vec2(Math.cos(body.getAngle).toFloat, Math.sin(body.getAngle).toFloat)
  def targetSpeed = SPEED * thrust * (if (direction.y * thrust < 0) direction.x else (2-direction.x))
  def targetVelocity = new Vec2(direction.x*targetSpeed, direction.y*targetSpeed)

  override def create(x: Float) {
    val y = server.ground.heightAt(x).toFloat - STARTING_ALTITUDE
    body.setXForm(new Vec2(x, y), 0)
    println("Setting tank mass.")
    body.setMassFromShapes

    super.create(x)
  }

  override def update(delta: Int) {
    super.update(delta)
    if (destroy) {
      for (i <- 0 until corbomite) {
        server.addProjectile(this, gun.x, gun.y, -50f+rand.nextFloat()*100f, rand.nextFloat()*150f, ProjectileTypes.CORBOMITE)
      }
      remove
    }
    if (isDead) return

    
    if (true) /*base.isTouchingStatic(new ArrayList[Body]) ||
        (wheel1.isTouchingStatic(new ArrayList[Body]) &&
         wheel2.isTouchingStatic(new ArrayList[Body]))) */{
      contactTime = contactGrace
    }
    else if (contactTime > 0) {
      contactTime -= delta
    }
    
    if (fallImmuneTimer >= 0) {
      fallImmuneTimer -= delta
    }

    if (lift != 0) {
      airborne = true
    } else if (grounded) {
      airborne = false
    }
    jumping = jumpFuel > 0 && (lift != 0 || (thrust != 0 && airborne))

    if (jumping) {
      applyJumpForces(delta)
    }
    else {
      regenJumpFuel(delta)
      if (grounded) {
        //applyGroundForces(delta)
      }
    }
  }

  def applyJumpForces(delta: Int) = {
    jumpFuel -= delta*3

    body.applyForce(new Vec2(airSpeedX * thrust, (airSpeedY * lift) + session.ground.heightAt(x) - y), 
                    body.getPosition)

    val targetRotation = airTilt * thrust

    body.setAngularVelocity(0)
  
    if (body.getAngle < targetRotation) {
      body.setAngularVelocity(airAngularSpeed)
    } 
    else if (body.getAngle > targetRotation) {
      body.setAngularVelocity(-airAngularSpeed)
    }
  }

  def applyGroundForces(delta: Int) {
    body.setLinearVelocity(targetVelocity)

    body.wakeUp
  }

  def regenJumpFuel(delta: Int) {
    jumpFuel += delta
    if (jumpFuel > purchasedJumpFuel) {
      jumpFuel = purchasedJumpFuel
    }
  }
  
  override def damage(d: Int, source: Projectile) {
    val oldHealth = health
    var damageDone = d

    if (!grounded) {
      val altitude = server.ground.heightAt(x) - y
      val scale = 1 + altitude/altitudeDamageFactor
      
      if (scale > 1) {
        damageDone = (damageDone * scale).toInt
        println(player.name + " hit in the air, damage adjusted to " + damageDone)
      }
    }
    health -= damageDone
    
    if (null != source) {
      source.tank.player.awardHit(this, damageDone)
    }
    
    if (isDead && oldHealth > 0) {
      if (null != source) {
        server.broadcastChat(source.tank.player.name + " killed " + player.name + " with " + source.getClass.getName + ".")
      }
      else {
        server.broadcastChat(player.name + " went splat.")
      }
      
      destroy = true
    }
    if (isDead) {
      health = 0
    }
  }

  override def remove = {
    println("Removing tank.")
    super.remove
    destroy = false
  }

  override def collide(other: GameObject, contact: ContactPoint) {
    if (other.isInstanceOf[Ground]) {
      if (body.getLinearVelocity.length > fallThreshold && fallImmuneTimer < 0) {
        println(player + " hit the ground at velocity " + body.getLinearVelocity);
        damage((body.getLinearVelocity.length.toInt - fallThreshold)/fallDamageDivider, null)
        fallImmuneTimer = fallImmuneTime
      }
    }
  }
  
  def serialise = {
    Operations.toByteArray((
      x.toFloat,
      y.toFloat,
      angle.toShort, 
      gun.angle.toShort, 
      gun.power.toShort,
      Math.ceil(gun.timer).toShort, 
      health.toShort, 
      jumping,
      gun.angleChange.toByte, 
      gun.powerChange.toByte,
      gun.selectedWeapon.id.toByte,
      gun.ammo(gun.selectedWeapon).toShort,
      jumpFuel.toShort,
      id
    ))
  }
 
}

