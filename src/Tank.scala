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

class Tank (session: Session, var id: Byte) extends Collider {
  val WIDTH = 30f
  val HEIGHT = WIDTH/2
  val TAPER = WIDTH/8
  val BEVEL = WIDTH/10

  val SPEED = WIDTH/2 // pixels/second

  val WHEEL_RADIUS = BEVEL
  val WHEEL_OFFSET_X = WIDTH/2-BEVEL
  val WHEEL_OFFSET_Y = -BEVEL

  val BODY_MASS = 1f
  val WHEEL_MASS = 1f
  
  val GUN_ANGLE_SPEED = 20 // degrees/second
  val GUN_POWER_SPEED = 50 // pixels/second/second

  val GUN_ANGLE_RANGE = new Range(-90, 90, 1)
  val GUN_POWER_RANGE = new Range(50, 300, 1)

  val GUN_POWER_SCALE = 200.0f

  val GUN_OFFSET_X = 0
  val GUN_OFFSET_Y = -(1.5f*HEIGHT)
  
  val GUN_READY_COLOR   = new Color(0.0f, 1.0f, 0.0f, 0.5f)
  val GUN_LOADING_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.5f)
    
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
                      new slick.geom.Vector2f(WIDTH/2-BEVEL, 0),
                      new slick.geom.Vector2f(-(WIDTH/2-BEVEL), 0),
                      new slick.geom.Vector2f(-WIDTH/2, -BEVEL)
                    ).toArray
  var player: Player = null

  var ammo = new HashMap[ProjectileTypes.Value, Int]()
  for (projectileType <- ProjectileTypes) {
    ammo(projectileType) = 0
  }
  ammo(ProjectileTypes.PROJECTILE) = 999

  var selectedWeapon = ProjectileTypes.PROJECTILE

  val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY())).toArray
  val tankShape = new slick.geom.Polygon(drawShapePoints)
  val arrowShape = new slick.geom.Polygon(List[Float](-5, 0, -5, -50, -10, -50, 0, -60, 10, -50, 5, -50, 5, 0).toArray)

  val physShapePoints = shapePoints.map((point) => new phys2d.math.Vector2f(point.x, point.y))
  val physShape = new phys2d.raw.shapes.Polygon(physShapePoints.toArray)

  val wheelShape = new phys2d.raw.shapes.Circle(WHEEL_RADIUS)
  var body: phys2d.raw.Body = _
  var wheel1: phys2d.raw.Body = _
  var wheel2: phys2d.raw.Body = _
  //body.setMaxVelocity(20f, 20f)

  var color: Color = _

  var thrust = 0
  var gunAngleChange = 0
  var gunPowerChange = 0

  var gunAngle = 0f
  var gunPower = 200f
  var gunTimer = 0f

  var health = 100

  val contactGrace = 50
  var contactTime = 0

  var destroy = false
  var firing = false
  var jumping = false

  var maxJumpFuel = 10000
  var jumpFuel = 1000
  def fuelPercent = (jumpFuel.toFloat/maxJumpFuel) * 100

  def grounded : Boolean = contactTime > 0; true

  def wheelColor = color // new Color(0f, 0f, 1f)

  def angle = body.getRotation.toDegrees
  def x = body.getPosition.getX
  def y = body.getPosition.getY
  def velocity = body.getVelocity

  def gunReady = gunTimer <= 0
  
  def gunX = x - GUN_OFFSET_X * Math.cos(angle.toRadians) - GUN_OFFSET_Y * Math.sin(angle.toRadians)
  def gunY = y - GUN_OFFSET_X * Math.sin(angle.toRadians) + GUN_OFFSET_Y * Math.cos(angle.toRadians)
  
  def isAlive = health > 0
  def isDead = !isAlive

  def direction = new phys2d.math.Vector2f(Math.cos(body.getRotation).toFloat,
                                           Math.sin(body.getRotation).toFloat)

  def targetSpeed = SPEED * thrust * (if (direction.getY * thrust < 0) direction.getX else (2-direction.getX))
  def targetVelocity = new phys2d.math.Vector2f(direction.getX*targetSpeed, direction.getY*targetSpeed)

  def actualSpeed = velocity.dot(direction)

  def speedDelta = targetSpeed - actualSpeed

  var previousValues: (Float, Float, Float, Float, Float, Int, Int, Int, Int, Int, Int) = _

  def create(x: Float) = {
    this.color = color
    
    if (session.isInstanceOf[Server]) {
      val y = session.ground.heightAt(x).toFloat
      body = new phys2d.raw.Body(physShape, BODY_MASS)
      body.setPosition(x, y - 100f)

      wheel1 = new phys2d.raw.Body(wheelShape, WHEEL_MASS)
      wheel2 = new phys2d.raw.Body(wheelShape, WHEEL_MASS)

      body.addExcludedBody(wheel1)
      body.addExcludedBody(wheel2)

      wheel1.setPosition(x-WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y)
      wheel2.setPosition(x+WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y)

      val joint1 = new phys2d.raw.BasicJoint(body, wheel1, new phys2d.math.Vector2f(x-WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y))
      val joint2 = new phys2d.raw.BasicJoint(body, wheel2, new phys2d.math.Vector2f(x+WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y))

      session.world.add(joint1)
      session.world.add(joint2)

      session.addBody(this, wheel1)
      session.addBody(this, wheel2)
    }
    else {
      body = new phys2d.raw.StaticBody(physShape)
      wheel1 = new phys2d.raw.StaticBody(wheelShape)
      wheel2 = new phys2d.raw.StaticBody(wheelShape)

      body.addExcludedBody(wheel1)
      body.addExcludedBody(wheel2)
    }
    
    //body.setFriction(0.8f)
    wheel1.setFriction(0.99f)
    wheel2.setFriction(0.99f)
    //body.setDamping(0.007f)

    session.addBody(this, body)
  }

  def cycleWeapon() {
    var id = (selectedWeapon.id + 1) % ProjectileTypes.maxId
    
    if (!ammo.values.exists((ammoType) => {ammoType > 0})) {
      println("No ammo left.")
      return
    }

    while(ammo(ProjectileTypes.apply(id)) <= 0) {
      id = (id + 1) % ProjectileTypes.maxId
    }

    selectedWeapon = ProjectileTypes.apply(id)
    println(selectedWeapon)
  }
  
  def update(delta: Int): Unit = {
    if (destroy) remove
    if (isDead) return
    if (firing) fire
    
    if (body.isTouchingStatic(new ArrayList[phys2d.raw.Body]) ||
        wheel1.isTouchingStatic(new ArrayList[phys2d.raw.Body]) ||
        wheel2.isTouchingStatic(new ArrayList[phys2d.raw.Body])) {
      contactTime = contactGrace
    }
    else if (contactTime > 0) {
      contactTime -= delta
    }

    if (jumping && jumpFuel > 0) {
      jumpFuel -= delta
      val force = new phys2d.math.Vector2f(Math.sin(body.getRotation).toFloat * 500,
                                           -Math.cos(body.getRotation).toFloat * 500)
      body.addForce(force)
    }
    else if (grounded) {
      val acceleration = new phys2d.math.Vector2f(Math.cos(body.getRotation).toFloat*speedDelta,
                                                  Math.sin(body.getRotation).toFloat*speedDelta)
      
      body.adjustVelocity(acceleration)

      body.setIsResting(false)
      wheel1.setIsResting(false)
      wheel2.setIsResting(false)

    }
    
    if (gunAngleChange != 0) {
      val newAngle = gunAngle + gunAngleChange * GUN_ANGLE_SPEED * delta / 1000.0f
      
      gunAngle = Math.max(GUN_ANGLE_RANGE.start, Math.min(GUN_ANGLE_RANGE.end, newAngle))
    }
    
    if (gunPowerChange != 0) {
      val newPower = gunPower + gunPowerChange * GUN_POWER_SPEED * delta / 1000.0f
      
      gunPower = Math.max(GUN_POWER_RANGE.start, Math.min(GUN_POWER_RANGE.end, newPower))
    }
    
    if (!gunReady) {
      gunTimer -= delta / 1000.0f
    }
  }
  
  def fire() {
    if (isDead) return
    
    if (gunReady && ammo(selectedWeapon) > 0) {
      ammo(selectedWeapon) = ammo(selectedWeapon) - 1
      val proj = session.addProjectile(this, gunX, gunY, angle+gunAngle, gunPower, selectedWeapon)
      gunTimer = proj.reloadTime
    }
    
    if (ammo(selectedWeapon) == 0) {
      cycleWeapon
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
      health = 0
    }
  }
  
  def remove =  {
    if (null != body) session.removeBody(body)
    if (null != wheel1) session.removeBody(wheel1)
    if (null != wheel2) session.removeBody(wheel2)
    destroy = false
  }

  def render(g: Graphics): Unit = {
    if (isDead) {
      return
    }
    
    g.setColor(color)
    
    // g.getFont.drawString(20, 0, wheel1.getAngularVelocity.toString)
    // g.getFont.drawString(20, 0, "targetSpeed = " + targetSpeed)
    // g.getFont.drawString(20, 10, "actualSpeed = " + actualSpeed)
    // g.getFont.drawString(20, 20, "speedDelta = " + speedDelta)
    
    g.translate(x, y)
    g.rotate(0, 0, angle)
    
    //Tank body
    g.fill(tankShape)
    
    //Tank gun arrow
    g.translate(GUN_OFFSET_X, GUN_OFFSET_Y)
    g.rotate(0, 0, gunAngle)
    g.scale(1, gunPower/GUN_POWER_SCALE)
    g.setColor(if (gunReady) GUN_READY_COLOR else GUN_LOADING_COLOR)
    g.fill(arrowShape)
    
    g.resetTransform

    drawWheel(g, -WHEEL_OFFSET_X, wheel1.getRotation)
    drawWheel(g, WHEEL_OFFSET_X, wheel2.getRotation)
  }

  def drawWheel(g : Graphics, offsetX : Float, rotation : Float) {
    g.translate(x, y)
    g.rotate(0, 0, body.getRotation.toDegrees)
    g.translate(offsetX, WHEEL_OFFSET_Y)
    
    g.setColor(wheelColor)
    g.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2)
    
    // g.rotate(0, 0, rotation)
    // 
    // g.setColor(new Color(1f, 0f, 0f))
    // g.drawLine(0, 0, WHEEL_RADIUS, 0)
    
    g.resetTransform
  }
  
  def currentValues = {
    (x, y, angle, gunAngle, gunPower, gunAngleChange, gunPowerChange, health, thrust, selectedWeapon.id, ammo(selectedWeapon))
  }

  def serialise = {
    Operations.toByteArray((
      x.toFloat,
      y.toFloat,
      angle.toShort, 
      gunAngle.toShort, 
      gunPower.toShort,
      Math.ceil(gunTimer).toShort, 
      health.toShort, 
      thrust.toByte, 
      gunAngleChange.toByte, 
      gunPowerChange.toByte,
      selectedWeapon.id.toByte,
      ammo(selectedWeapon).toShort,
      jumpFuel.toShort,
      id
    ))
  }
  
  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[(Float, Float, Short, Short, Short, Short, Short, Byte, Byte, Byte, Byte, Short, Short, Byte)](data)
    
    val (newX, newY, newAngle, 
        newGunAngle, newGunPower, newGunTimer, 
        newHealth, newThrust, newGunAngleChange, newGunPowerChange, 
        newSelectedWeapon, newSelectedAmmo, newFuel,
        newID) = values

    body.setPosition(newX, newY)
    body.setRotation(newAngle.toFloat.toRadians)
    gunAngle = newGunAngle
    gunPower = newGunPower
    gunTimer = newGunTimer
    health = newHealth
    thrust = newThrust
    gunAngleChange = newGunAngleChange
    gunPowerChange = newGunPowerChange
    selectedWeapon = ProjectileTypes.apply(newSelectedWeapon)
    ammo(selectedWeapon) = newSelectedAmmo
    jumpFuel = newFuel
    
    id = newID
    
    color = BODY_COLORS(id%BODY_COLORS.length)
  }

}
