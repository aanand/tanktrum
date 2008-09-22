import sbinary.Instances._
import sbinary.Operations

import java.util.ArrayList

import net.phys2d.raw.shapes._
import net.phys2d.raw._
import net.phys2d.math._

object ServerTank {
  implicit def tankToServerTank(tank: Tank) = tank.asInstanceOf[ServerTank]
}
class ServerTank(server: Server, id: Byte) extends Tank(server, id) {
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

  def direction = new Vector2f(Math.cos(body.getRotation).toFloat, Math.sin(body.getRotation).toFloat)
  def targetSpeed = SPEED * thrust * (if (direction.getY * thrust < 0) direction.getX else (2-direction.getX))
  def targetVelocity = new Vector2f(direction.getX*targetSpeed, direction.getY*targetSpeed)

  override def create(x: Float) {
    val y = server.ground.heightAt(x).toFloat - STARTING_ALTITUDE
    body = new Body(physShape, BODY_MASS)
    body.setPosition(x, y)

    wheel1 = new Body(wheelShape, WHEEL_MASS)
    wheel2 = new Body(wheelShape, WHEEL_MASS)
    base = new Body(baseShape, BASE_MASS)

    //I see why excluded body groups in Box2D are a good idea.
    body.addExcludedBody(wheel1)
    body.addExcludedBody(wheel2)
    body.addExcludedBody(base)
    base.addExcludedBody(wheel1)
    base.addExcludedBody(wheel2)

    wheel1.setPosition(x-WHEEL_OFFSET_X, y+WHEEL_OFFSET_Y)
    wheel2.setPosition(x+WHEEL_OFFSET_X, y+WHEEL_OFFSET_Y)
    base.setPosition(x+BASE_OFFSET_X, y+BASE_OFFSET_Y)

    val joint1 = new FixedJoint(body, wheel1)
    val joint2 = new FixedJoint(body, wheel2)
    val anc1 = new Vector2f(x+BASE_OFFSET_X-BASE_WIDTH/3, y+BASE_OFFSET_Y)
    val anc2 = new Vector2f(x+BASE_OFFSET_X+BASE_WIDTH/3, y+BASE_OFFSET_Y)

    //Apparantly a FixedJoint doesn't actually fix the angle.
    //Trying a FixedAngleJoint made tanks fly off at high speeds.
    //This will do I guess.
    val baseJoint1 = new BasicJoint(body, base, anc1)
    val baseJoint2 = new BasicJoint(body, base, anc2)

    server.world.add(joint1)
    server.world.add(joint2)
    server.world.add(baseJoint1)
    server.world.add(baseJoint2)

    server.addBody(this, wheel1)
    server.addBody(this, wheel2)
    server.addBody(this, base)
    
    body.setFriction(1f)
    base.setFriction(0.8f)
    wheel1.setFriction(0f)
    wheel2.setFriction(0f)
    super.create(x)
  }

  override def update(delta: Int) {
    super.update(delta)
    if (destroy) remove
    if (isDead) return
    
    if (base.isTouchingStatic(new ArrayList[Body]) ||
        (wheel1.isTouchingStatic(new ArrayList[Body]) &&
         wheel2.isTouchingStatic(new ArrayList[Body]))) {
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
    body.setRotation(base.getRotation)
    jumping = jumpFuel > 0 && (lift != 0 || (thrust != 0 && airborne))

    if (jumping) {
      applyJumpForces(delta)
    }
    else {
      regenJumpFuel(delta)
      if (grounded) {
        applyGroundForces(delta)
      }
    }
  }

  def applyJumpForces(delta: Int) = {
    jumpFuel -= delta*3

    body.addForce(new Vector2f(airSpeedX * thrust, (airSpeedY * lift) + session.ground.heightAt(x) - y))

    val targetRotation = airTilt * thrust

    body.adjustAngularVelocity(-body.getAngularVelocity)
  
    if (body.getRotation < targetRotation) {
      body.adjustAngularVelocity(airAngularSpeed)
    } 
    else if (body.getRotation > targetRotation) {
      body.adjustAngularVelocity(-airAngularSpeed)
    }
  }

  def applyGroundForces(delta: Int) {
    body.adjustVelocity(new Vector2f(-body.getVelocity.getX, -body.getVelocity.getY));
    body.adjustVelocity(targetVelocity)

    body.setIsResting(false)
    wheel1.setIsResting(false)
    wheel2.setIsResting(false)
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
      
      val rand = new Random
      for (i <- 0 until corbomite) {
        server.addProjectile(this, gun.x, gun.y, -50f+rand.nextFloat()*100f, rand.nextFloat()*150f, ProjectileTypes.CORBOMITE)
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

  override def collide(other : Collider, event: CollisionEvent) {
    if (other.isInstanceOf[Ground]) {
      if (body.getVelocity.length > fallThreshold && fallImmuneTimer < 0) {
        println(player + " hit the ground at velocity " + body.getVelocity);
        damage((body.getVelocity.length.toInt - fallThreshold)/fallDamageDivider, null)
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

