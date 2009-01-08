package server

import shared._

import sbinary.Instances._
import sbinary.Operations

import java.util.ArrayList

import Math._

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

class Tank(val server: Server, val id: Byte) extends GameObject(server) {
  lazy val WIDTH  = Config("tank.width").toFloat
  lazy val HEIGHT = Config("tank.height").toFloat
  lazy val TAPER  = Config("tank.taper").toFloat
  lazy val BEVEL  = Config("tank.bevel").toFloat
  
  lazy val WHEEL_RADIUS = BEVEL
  lazy val WHEEL_OFFSET_X = WIDTH/2-BEVEL
  lazy val WHEEL_OFFSET_Y = -BEVEL

  lazy val BASE_WIDTH = WIDTH - 2*WHEEL_RADIUS
  lazy val BASE_HEIGHT = BEVEL
  lazy val BASE_OFFSET_X = 0
  lazy val BASE_OFFSET_Y = -BASE_HEIGHT/2
  lazy val friction = Config("tank.friction").toFloat
  
  var health = 100f
  
  val contactGrace = 50
  var contactTime = 0

  var jumping = false
  var airborne = false

  var maxJumpFuel = Config("tank.jumpjet.maxFuel").toInt
  var purchasedJumpFuel = Config("tank.jumpjet.startingFuel").toInt
  var jumpFuel = 0f
  var jumpFuelBurn = Config("tank.jumpjet.burn").toFloat
  var jumpFuelRegen = Config("tank.jumpjet.regen").toFloat
  
  var corbomite = 0
  val maxCorbomite = Config("tank.maxCorbomite").toInt

  var topShape: Shape = _
  var baseShape: Shape = _
  var wheelShape1: Shape = _
  var wheelShape2: Shape = _

  var missile: Missile = _

  var player: Player = _
  
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
  
  val gun = new Gun(server, this)

  lazy val shapePoints = List[Vec2] (
                      new Vec2(-(WIDTH/2-TAPER), -HEIGHT),
                      new Vec2(WIDTH/2-TAPER, -HEIGHT),
                      new Vec2(WIDTH/2, -BEVEL),
                      new Vec2(-WIDTH/2, -BEVEL)
                    ).toArray
  
  override def shapes = {
    val bodyShapeDefPoints = shapePoints.map((point) => new Vec2(point.x, point.y))
    val bodyShapeDef = new PolygonDef
    bodyShapeDefPoints.foreach(bodyShapeDef.addVertex(_))
    bodyShapeDef.density = 1f
    bodyShapeDef.restitution = 0f
    bodyShapeDef.friction = friction

    val baseShapeDef = new PolygonDef
    baseShapeDef.setAsBox(BASE_WIDTH/2, BASE_HEIGHT/2, new Vec2(BASE_OFFSET_X, BASE_OFFSET_Y), 0f)
    baseShapeDef.density = 5f
    baseShapeDef.restitution = 0f
    baseShapeDef.friction = friction
    
    val wheelShapeDef1 = new CircleDef
    wheelShapeDef1.radius = WHEEL_RADIUS
    wheelShapeDef1.localPosition = new Vec2(WHEEL_OFFSET_X, WHEEL_OFFSET_Y)
    wheelShapeDef1.density = 5f
    wheelShapeDef1.restitution = 0f
    wheelShapeDef1.friction = friction
    
    val wheelShapeDef2 = new CircleDef
    wheelShapeDef2.radius = WHEEL_RADIUS
    wheelShapeDef2.localPosition = new Vec2(-WHEEL_OFFSET_X, WHEEL_OFFSET_Y)
    wheelShapeDef2.density = 5f
    wheelShapeDef2.restitution = 0f
    wheelShapeDef2.friction = friction

    List(bodyShapeDef, baseShapeDef, wheelShapeDef1, wheelShapeDef2)
  }

  override def loadShapes = {
    val shapesList = shapes
    topShape = body.createShape(shapesList(0))
    baseShape = body.createShape(shapesList(1))
    wheelShape1 = body.createShape(shapesList(2))
    wheelShape2 = body.createShape(shapesList(3))
    body.setMassFromShapes
  }

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
  
  def fuelPercent = (jumpFuel.toFloat/maxJumpFuel) * 100

  def grounded : Boolean = contactTime > 0

  def angle = body.getAngle.toDegrees
  def velocity = body.getLinearVelocity

  def isAlive = health > 0
  def isDead = !isAlive

  def create(x: Float) {
    val y = server.ground.heightAt(x).toFloat - STARTING_ALTITUDE
    body.setXForm(new Vec2(x, y), 0)
  }

  def update(delta: Int) {
    //Keep the body angle between -Pi and Pi:
    if (body.getAngle > Pi) {
      body.setXForm(body.getPosition, body.getAngle - 2*Pi.toFloat)
    }
    else if (body.getAngle < -Pi) {
      body.setXForm(body.getPosition, body.getAngle + 2*Pi.toFloat)
    }
    
    gun.update(delta)

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
  
  def damage(d: Float, source: Projectile) {
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
        server.broadcastChat(source.tank.player.name + " killed " + player.name + " with " + source.name + ".")
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

  def remove = {
    println("Removing tank.")
    if (null != body) server.removeBody(body)
    destroy = false
  }

  override def collide(other: GameObject, contact: ContactPoint) {
    if (other.isInstanceOf[Ground]) {
      if (body.getLinearVelocity.length > fallThreshold && fallImmuneTimer < 0) {
        println(player + " hit the ground at velocity " + body.getLinearVelocity)
        damage((body.getLinearVelocity.length.toInt - fallThreshold)/fallDamageDivider, null)
        fallImmuneTimer = fallImmuneTime
      }
    }
  }

  override def persist(other: GameObject, contact: ContactPoint) {
    if (other == server.ground) {
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

