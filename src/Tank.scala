import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import net.phys2d.raw.shapes._
import net.phys2d.math._
import net.phys2d

import java.util.ArrayList
import scala.collection.mutable.HashMap

import sbinary.Instances._
import sbinary.Operations

import SwitchableParticleEmitter._

class Tank (session: Session, var id: Byte) extends Collider {
  val WIDTH = 30f
  val HEIGHT = WIDTH/2
  val TAPER = WIDTH/8
  val BEVEL = WIDTH/10

  val SPEED = WIDTH/2 // pixels/second

  val STARTING_ALTITUDE = 100f

  val airSpeedX = 250
  val airSpeedY = 500
  val airTilt = Math.toRadians(10f).toFloat
  val airAngularSpeed = Math.toRadians(45f).toFloat

  val WHEEL_RADIUS = BEVEL
  val WHEEL_OFFSET_X = WIDTH/2-BEVEL
  val WHEEL_OFFSET_Y = -BEVEL

  val BASE_WIDTH = WIDTH - 2*WHEEL_RADIUS
  val BASE_HEIGHT = BEVEL
  val BASE_OFFSET_X = 0
  val BASE_OFFSET_Y = -BASE_HEIGHT/2

  val BODY_MASS = 2.8f
  val WHEEL_MASS = 0.1f
  val BASE_MASS = 0.1f
  
  val gun = new Gun(session, this)
  
  val BODY_COLORS = List(
    new Color(1f, 0f, 0f),
    new Color(0f, 1f, 0f),
    new Color(0f, 0f, 1f),
    new Color(1f, 1f, 0f),
    new Color(1f, 0f, 1f),
    new Color(0f, 1f, 1f))
 
  val shapePoints = List[slick.geom.Vector2f] (
                      new slick.geom.Vector2f(-(WIDTH/2-TAPER), -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2-TAPER, -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2, -BEVEL),
                      new slick.geom.Vector2f(-WIDTH/2, -BEVEL)
                    ).toArray
  var player: Player = null

  val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY())).toArray
  val tankShape = new slick.geom.Polygon(drawShapePoints)

  val physShapePoints = shapePoints.map((point) => new phys2d.math.Vector2f(point.x, point.y))
  val physShape = new phys2d.raw.shapes.Polygon(physShapePoints.toArray)

  val baseShape = new phys2d.raw.shapes.Box(BASE_WIDTH, BASE_HEIGHT);

  val wheelShape = new phys2d.raw.shapes.Circle(WHEEL_RADIUS)
  var body: phys2d.raw.Body = _
  var wheel1: phys2d.raw.Body = _
  var wheel2: phys2d.raw.Body = _
  var base: phys2d.raw.Body = _

  var color: Color = _

  var thrust = 0
  var lift = 0
  var health = 100

  val contactGrace = 50
  var contactTime = 0

  var destroy = false
  
  var jumping = false
  var airborne = false

  var maxJumpFuel = 20000
  var purchasedJumpFuel = 2000
  var jumpFuel = 0
  
  def fuelPercent = (jumpFuel.toFloat/maxJumpFuel) * 100

  def grounded : Boolean = contactTime > 0; true

  def wheelColor = color

  def angle = body.getRotation.toDegrees
  def x = body.getPosition.getX
  def y = body.getPosition.getY
  def velocity = body.getVelocity

  def isAlive = health > 0
  def isDead = !isAlive

  def direction = new phys2d.math.Vector2f(Math.cos(body.getRotation).toFloat,
                                           Math.sin(body.getRotation).toFloat)

  def targetSpeed = SPEED * thrust * (if (direction.getY * thrust < 0) direction.getX else (2-direction.getX))
  def targetVelocity = new phys2d.math.Vector2f(direction.getX*targetSpeed, direction.getY*targetSpeed)

  var previousValues: (Float, Float, Float, Float, Float, Int, Int, Int, Int, Boolean, Int, Int, Int) = _

  var jetEmitter: slick.particles.ConfigurableEmitter = _
  var vapourEmitter: slick.particles.ConfigurableEmitter = _

  def particleEmitters = List(jetEmitter, vapourEmitter)

  var emitting = false

  def create(x: Float) = {
    this.color = color
    
    if (session.isInstanceOf[Server]) {
      val y = session.ground.heightAt(x).toFloat - STARTING_ALTITUDE
      body = new phys2d.raw.Body(physShape, BODY_MASS)
      body.setPosition(x, y)

      wheel1 = new phys2d.raw.Body(wheelShape, WHEEL_MASS)
      wheel2 = new phys2d.raw.Body(wheelShape, WHEEL_MASS)
      base = new phys2d.raw.Body(baseShape, BASE_MASS)

      //I see why excluded body groups in Box2D are a good idea.
      body.addExcludedBody(wheel1)
      body.addExcludedBody(wheel2)
      body.addExcludedBody(base)
      base.addExcludedBody(wheel1)
      base.addExcludedBody(wheel2)

      wheel1.setPosition(x-WHEEL_OFFSET_X, y+WHEEL_OFFSET_Y)
      wheel2.setPosition(x+WHEEL_OFFSET_X, y+WHEEL_OFFSET_Y)
      base.setPosition(x+BASE_OFFSET_X, y+BASE_OFFSET_Y)

      val joint1 = new phys2d.raw.FixedJoint(body, wheel1)
      val joint2 = new phys2d.raw.FixedJoint(body, wheel2)
      val anc1 = new phys2d.math.Vector2f(x+BASE_OFFSET_X-BASE_WIDTH/3, y+BASE_OFFSET_Y)
      val anc2 = new phys2d.math.Vector2f(x+BASE_OFFSET_X+BASE_WIDTH/3, y+BASE_OFFSET_Y)

      //Apparantly a FixedJoint doesn't actually fix the angle.
      //Trying a FixedAngleJoint made tanks fly off at high speeds.
      //This will do I guess.
      val baseJoint1 = new phys2d.raw.BasicJoint(body, base, anc1)
      val baseJoint2 = new phys2d.raw.BasicJoint(body, base, anc2)

      session.world.add(joint1)
      session.world.add(joint2)
      session.world.add(baseJoint1)
      session.world.add(baseJoint2)

      session.addBody(this, wheel1)
      session.addBody(this, wheel2)
      session.addBody(this, base)
    }
    else {
      body = new phys2d.raw.StaticBody(physShape)
      wheel1 = new phys2d.raw.StaticBody(wheelShape)
      wheel2 = new phys2d.raw.StaticBody(wheelShape)
      base = new phys2d.raw.StaticBody(baseShape)

      jetEmitter = slick.particles.ParticleIO.loadEmitter("media/particles/jet.xml")
      vapourEmitter = slick.particles.ParticleIO.loadEmitter("media/particles/vapour.xml")
      
      for (e <- particleEmitters) {
        session.asInstanceOf[Client].particleSystem.addEmitter(e)
        e.setEmitting(false)
      }
    }
    
    body.setFriction(1f)
    base.setFriction(0.8f)
    wheel1.setFriction(0f)
    wheel2.setFriction(0f)

    session.addBody(this, body)
  }

  def update(delta: Int): Unit = {
    if (destroy) remove
    if (isDead) return
    
    gun.update(delta)
    
    if (base.isTouchingStatic(new ArrayList[phys2d.raw.Body]) ||
        (wheel1.isTouchingStatic(new ArrayList[phys2d.raw.Body]) &&
         wheel2.isTouchingStatic(new ArrayList[phys2d.raw.Body]))) {
      contactTime = contactGrace
    }
    else if (contactTime > 0) {
      contactTime -= delta
    }

    if (lift != 0) {
      airborne = true
    } else if (grounded) {
      airborne = false
    }
    
    if (session.isInstanceOf[Server]) {
      body.setRotation(base.getRotation)
      jumping = jumpFuel > 0 && (lift != 0 || (thrust != 0 && airborne))
    }
    
    if (jumping) {
      jumpFuel -= delta*3

      body.addForce(new phys2d.math.Vector2f(airSpeedX * thrust, (airSpeedY * lift) + session.ground.heightAt(x) - y))

      val targetRotation = airTilt * thrust

      body.adjustAngularVelocity(-body.getAngularVelocity)
      
      if (body.getRotation < targetRotation) {
        body.adjustAngularVelocity(airAngularSpeed)
      } else if (body.getRotation > targetRotation) {
        body.adjustAngularVelocity(-airAngularSpeed)
      }
      
      if (session.isInstanceOf[Client]) {
        if (!emitting) {
          for (e <- particleEmitters) {
            e.setEmitting(true)
          }
          
          emitting = true
        }
        
        for (e <- particleEmitters) { e.setPosition(x, y) }
      }
    } else {
      if (session.isInstanceOf[Server]) {
        jumpFuel += delta
        if (jumpFuel > purchasedJumpFuel) {
          jumpFuel = purchasedJumpFuel
        }
        if (grounded) {
            body.adjustVelocity(new phys2d.math.Vector2f(-body.getVelocity.getX, -body.getVelocity.getY));
            body.adjustVelocity(targetVelocity)

            body.setIsResting(false)
            wheel1.setIsResting(false)
            wheel2.setIsResting(false)

        }
      }
      else {
        if (emitting) {
          for (e <- particleEmitters) {
            e.setEmitting(false)
          }
          
          emitting = false
        }
      }
    }
  }
  
  
  def damage(d: Int, source: Projectile) {
    val oldHealth = health

    health -= d
    
    if (isDead && oldHealth > 0) {
      if (session.isInstanceOf[Server]) {
        session.asInstanceOf[Server].broadcastChat(source.tank.player.name + " killed " + player.name + " with " + source.getClass.getName + ".")
      }
      destroy = true
    }
    if (isDead) {
      health = 0
    }
  }
  
  def remove = {
    if (null != body) session.removeBody(body)
    if (null != wheel1) session.removeBody(wheel1)
    if (null != wheel2) session.removeBody(wheel2)
    if (null != base) session.removeBody(base)
    destroy = false
  }

  def render(g: Graphics) {
    if (isDead) {
      return
    }
    
    g.setColor(color)
    
    g.translate(x, y)
    g.rotate(0, 0, angle)
    
    //Tank body
    g.fill(tankShape)
    
    gun.render(g)
    
    drawWheel(g, -WHEEL_OFFSET_X, wheel1.getRotation)
    drawWheel(g, WHEEL_OFFSET_X, wheel2.getRotation)
    drawBase(g)

  }

  def drawBase(g: Graphics) {
    g.translate(x, y)
    g.rotate(0, 0, body.getRotation.toDegrees)
    g.translate(BASE_OFFSET_X, BASE_OFFSET_Y)
    g.fillRect(-BASE_WIDTH/2, -BASE_HEIGHT/2, BASE_WIDTH, BASE_HEIGHT)
    g.resetTransform
  }

  def drawWheel(g : Graphics, offsetX : Float, rotation : Float) {
    g.translate(x, y)
    g.rotate(0, 0, body.getRotation.toDegrees)
    g.translate(offsetX, WHEEL_OFFSET_Y)
    
    g.setColor(wheelColor)
    g.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2)
    
    g.resetTransform
  }
  
  def currentValues = {
    (x, y, angle, 
     gun.angle, gun.power, 
     gun.angleChange, gun.powerChange, 
     health, thrust, jumping, jumpFuel, 
     gun.selectedWeapon.id, 
     gun.ammo(gun.selectedWeapon))
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
      thrust.toByte,
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
      Float, Float, Short,  //x, y, angle
      Short, Short, Short,  //gun angle, gun power, gun timer
      Short, Byte, Boolean, //health, thrust, jumping
      Byte, Byte,           //gun angle change, gun power change
      Byte, Short, Short,   //selected weapon, selected ammo, jump fuel
      Byte)](data)          //id
    
    val (newX, newY, newAngle, 
        newGunAngle, newGunPower, newGunTimer, 
        newHealth, newThrust, newJumping,
        newGunAngleChange, newGunPowerChange, 
        newSelectedWeapon, newSelectedAmmo, newFuel,
        newID) = values

    body.setPosition(newX, newY)
    body.setRotation(newAngle.toFloat.toRadians)
    gun.angle = newGunAngle
    gun.power = newGunPower
    gun.timer = newGunTimer
    health = newHealth
    thrust = newThrust
    jumping = newJumping
    gun.angleChange = newGunAngleChange
    gun.powerChange = newGunPowerChange
    gun.selectedWeapon = ProjectileTypes.apply(newSelectedWeapon)
    gun.ammo(gun.selectedWeapon) = newSelectedAmmo
    jumpFuel = newFuel
    
    id = newID
    
    color = BODY_COLORS(id%BODY_COLORS.length)
  }
}

