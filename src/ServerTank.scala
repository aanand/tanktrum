import sbinary.Instances._
import sbinary.Operations

import java.util.ArrayList

import Math._

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
  val airTilt         = toRadians(Config("tank.air.tilt").toFloat).toFloat
  val airAngularSpeed = toRadians(Config("tank.air.angularSpeed").toFloat).toFloat

  val missileThrust   = Config("missile.thrust").toFloat

  var altitudeDamageFactor = Config("tank.air.altitudeDamageFactor").toFloat
  
  val fallThreshold     = Config("tank.fall.threshold").toInt
  val fallDamageDivider = Config("tank.fall.damageDivider").toInt
  val fallImmuneTime    = Config("tank.fall.immuneTime").toInt
  var fallImmuneTimer = fallImmuneTime

  var thrust = 0
  var lift = 0
  var destroy = false

  var wheel1OnGround = false
  var wheel2OnGround = false

  var previousValues: (Float, Float, Float, Float, Float, Int, Int, Int, Int, Boolean, Int, Int, Int) = _
  
  def currentValues = {
    (x, y, angle, 
     gun.angle, gun.power, 
     gun.angleChange, gun.powerChange, 
     health.toInt, thrust, jumping, jumpFuel.toInt, 
     gun.selectedWeapon.id, 
     gun.ammo(gun.selectedWeapon))
  }

  def direction = new Vec2(cos(body.getAngle).toFloat, sin(body.getAngle).toFloat)
  def targetSpeed = SPEED * thrust * (if (direction.y * thrust < 0) direction.x else (2-direction.x))
  def targetVelocity = new Vec2(direction.x*targetSpeed, direction.y*targetSpeed)

  override def create(x: Float) {
    val y = server.ground.heightAt(x).toFloat - STARTING_ALTITUDE
    body.setXForm(new Vec2(x, y), 0)
    super.create(x)
  }

  override def update(delta: Int) {

    //Keep the body angle between -Pi and Pi:
    if (body.getAngle > Pi) {
      body.setXForm(body.getPosition, body.getAngle - 2*Pi.toFloat)
    }
    else if (body.getAngle < -Pi) {
      body.setXForm(body.getPosition, body.getAngle + 2*Pi.toFloat)
    }

    super.update(delta)
    if (destroy) {
      health = 0
      for (i <- 0 until corbomite) {
        server.addProjectile(this, x+sin(body.getAngle).toFloat*HEIGHT/2, y-cos(body.getAngle).toFloat*HEIGHT/2, 
                              -40f+rand.nextFloat*80f, 
                                   rand.nextFloat*50f+body.getLinearVelocity.y*2, 
                              ProjectileTypes.CORBOMITE)
      }
      remove
    }
    if (isDead) return

    if (y < -Main.GAME_HEIGHT+HEIGHT) {
      server.broadcastChat(player.name + " went into orbit.")
      destroy = true
    }

    if (y > Main.GAME_HEIGHT || x < -WIDTH || x > Main.GAME_WIDTH+WIDTH) {
      server.broadcastChat(player.name + " left the world.")
      destroy = true
    }

    if (contactTime > 0) {
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

    if (null != missile) {
      applyMissleDirection(delta)
    }
    else if (jumping) {
      applyJumpForces(delta)
    }
    else {
      regenJumpFuel(delta)
      if (grounded) {
        applyGroundForces(delta)
      }
    }

    wheel1OnGround = false
    wheel2OnGround = false
  }

  def applyMissleDirection(delta: Int) = {
    missile.body.applyForce(new Vec2(missileThrust * thrust * cos(missile.body.getAngle).toFloat, 
                                     missileThrust * thrust * sin(missile.body.getAngle).toFloat), 
                            missile.body.getPosition)
  }

  def applyJumpForces(delta: Int) = {
    jumpFuel -= delta*jumpFuelBurn
    val force = new Vec2(airSpeedX * thrust, airSpeedY * lift)

    body.applyForce(force, body.getWorldPoint(body.getLocalCenter.add(new Vec2(0f, -HEIGHT/2))))

    val targetRotation = airTilt * thrust
    
    if (abs(body.getAngle - targetRotation) < Pi/2) {
      if (abs(body.getAngle - targetRotation) < 0.01) {
        body.setAngularVelocity(0f)
      }
      else if (body.getAngle < targetRotation) {
        body.setAngularVelocity(airAngularSpeed)
      } 
      else if (body.getAngle > targetRotation) {
        body.setAngularVelocity(-airAngularSpeed)
      }
    }
  }

  def applyGroundForces(delta: Int) {
    body.setLinearVelocity(targetVelocity)

    body.wakeUp
  }

  def regenJumpFuel(delta: Int) {
    jumpFuel += delta * jumpFuelRegen
    if (jumpFuel > purchasedJumpFuel) {
      jumpFuel = purchasedJumpFuel
    }
  }
  
  override def damage(d: Float, source: Projectile) {
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
      source.tank.player.awardHit(this, damageDone.toInt)
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

  override def persist(other: GameObject, contact: ContactPoint) {
    if (other == server.ground || other.isInstanceOf[ChatBox]) {
      if (contact.shape1 == baseShape || 
          contact.shape2 == baseShape) {
        contactTime = contactGrace
      }
      
      if (contact.shape1 == wheelShape1 ||
          contact.shape2 == wheelShape1) {
        wheel1OnGround = true
      }
      if (contact.shape1 == wheelShape2 ||
          contact.shape2 == wheelShape2) {
        wheel2OnGround = true
      }

      if (wheel1OnGround && wheel2OnGround) {
        contactTime = contactGrace
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
      ceil(gun.timer).toShort, 
      health,
      jumping,
      gun.angleChange.toByte, 
      gun.powerChange.toByte,
      gun.selectedWeapon.id.toByte,
      gun.ammo(gun.selectedWeapon).toShort,
      jumpFuel.toShort,
      id
    ))
  }

  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[(
      Short, Byte,         //gun angle, gun angle change
      Short, Byte)](data)  //gun power, gun power change
    
    val (newGunAngle, newGunAngleChange, newGunPower, newGunPowerChange) = values

    gun.angle = newGunAngle
    gun.power = newGunPower
    gun.angleChange = newGunAngleChange
    gun.powerChange = newGunPowerChange
  }
 
}

