import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import net.phys2d.raw.shapes._
import net.phys2d.math._
import net.phys2d

import java.util.ArrayList

import sbinary.Instances._
import sbinary.Operations

class Tank (session: Session) extends Collider {
  val WIDTH = 40f
  val HEIGHT = 20f
  val TAPER = 5f
  val BEVEL = 4f

  val SPEED = 20f // pixels/second

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

  val GUN_RELOAD_TIME = 4.0f
  
  val GUN_OFFSET_X = 0
  val GUN_OFFSET_Y = -30
  
  val GUN_READY_COLOR   = new Color(0.0f, 1.0f, 0.0f, 0.5f)
  val GUN_LOADING_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.5f)
  
  val shapePoints = List[slick.geom.Vector2f] (
                      new slick.geom.Vector2f(-(WIDTH/2-TAPER), -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2-TAPER, -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2, -BEVEL),
                      new slick.geom.Vector2f(WIDTH/2-BEVEL, 0),
                      new slick.geom.Vector2f(-(WIDTH/2-BEVEL), 0),
                      new slick.geom.Vector2f(-WIDTH/2, -BEVEL)
                    ).toArray

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

  def create(x: Float, color: Color) = {
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
  
  def update(delta: Int): Unit = {
    if (isDead) {
      return
    }
    
    if (body.isTouchingStatic(new ArrayList[phys2d.raw.Body]) ||
        wheel1.isTouchingStatic(new ArrayList[phys2d.raw.Body]) ||
        wheel2.isTouchingStatic(new ArrayList[phys2d.raw.Body])) {
      contactTime = contactGrace
    }
    else if (contactTime > 0) {
      contactTime -= delta
    }
    
    if (grounded) {
      val delta = new phys2d.math.Vector2f(Math.cos(body.getRotation).toFloat*speedDelta,
                                           Math.sin(body.getRotation).toFloat*speedDelta)
      
      body.adjustVelocity(delta)

      body.setIsResting(false)
      wheel1.setIsResting(false)
      wheel2.setIsResting(false)

      //body.addForce(new phys2d.math.Vector2f(0, -95f))
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
    
    if (gunReady) {
      session.addProjectile(this, gunX, gunY, angle+gunAngle, gunPower)
      gunTimer = GUN_RELOAD_TIME
    }
  }
  
  def damage(d: Int) {
    health -= d
    
    if (isDead) {
      kill
    }
  }
  
  def kill = {
    health = 0
    session.removeBody(body)
    
    if (null != wheel1) session.removeBody(wheel1)
    if (null != wheel2) session.removeBody(wheel2)

    //session.addFrag(new Frag(x - WHEEL_OFFSET_X, y + WHEEL_OFFSET_Y, WHEEL_RADIUS, color))
    //session.addFrag(new Frag(x + WHEEL_OFFSET_X, y + WHEEL_OFFSET_Y, WHEEL_RADIUS, color))
  }
  
  def render(g: Graphics): Unit = {
    if (isDead) {
      return
    }
    
    //g.fillRect(10 + (playerId-1)*110, 10, health, 10)
    
    // if (grounded) {
    //   g.setColor(color)
    // } else {
    //   g.setColor(new Color(1f, 1f, 1f))
    // }
    
    g.setColor(color)
    
    // g.getFont.drawString(20, 0, wheel1.getAngularVelocity.toString)
    
    // g.getFont.drawString(20, 0, "targetSpeed = " + targetSpeed)
    // g.getFont.drawString(20, 10, "actualSpeed = " + actualSpeed)
    // g.getFont.drawString(20, 20, "speedDelta = " + speedDelta)
    
    g.translate(x, y)
    g.rotate(0, 0, angle)
    
    g.fill(tankShape)
    
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
  
  def serialise = {
    Operations.toByteArray(List[Float](
      x.toFloat,
      y.toFloat,
      angle.toFloat, 
      gunAngle.toFloat, 
      gunPower.toFloat,
      gunTimer.toFloat, 
      health.toFloat, 
      thrust.toFloat, 
      gunAngleChange.toFloat, 
      gunPowerChange.toFloat,
      color.r.toFloat,
      color.g.toFloat,
      color.b.toFloat
    ))
  }
  
  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[List[Float]](data).elements
    
    body.setPosition(values.next, values.next)
    body.setRotation(values.next.toRadians)
    gunAngle = values.next
    gunPower = values.next
    gunTimer = values.next
    health = values.next.toInt
    thrust = values.next.toInt
    gunAngleChange = values.next.toInt
    gunPowerChange = values.next.toInt
    
    color = new slick.Color(values.next, values.next, values.next)
  }

}
