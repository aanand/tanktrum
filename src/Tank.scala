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

  val SPEED = 400f

  val WHEEL_OFFSET_X = WIDTH/4
  val WHEEL_OFFSET_Y = HEIGHT/4
  val WHEEL_RADIUS = 8
  
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
                      new slick.geom.Vector2f(WIDTH/2-TAPER/3, -HEIGHT/4),
                      new slick.geom.Vector2f(WIDTH/3, 0),
                      new slick.geom.Vector2f(-WIDTH/3, 0),
                      new slick.geom.Vector2f(-(WIDTH/2-TAPER/3), -HEIGHT/4)
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

  def angle = body.getRotation.toDegrees
  def x = body.getPosition.getX
  def y = body.getPosition.getY

  def create(x: Float, color: Color) = {
    val y = session.ground.heightAt(x).toFloat
    this.color = color
    
    if (session.isInstanceOf[Server]) {
      body = new phys2d.raw.Body(physShape, 1.0f)
      body.setPosition(x, y - 100f)

      wheel1 = new phys2d.raw.Body(wheelShape, 1)
      wheel2 = new phys2d.raw.Body(wheelShape, 1)

      body.addExcludedBody(wheel1)
      body.addExcludedBody(wheel2)

      wheel1.setPosition(x-WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y)
      wheel2.setPosition(x+WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y)

      val joint1 = new phys2d.raw.BasicJoint(body, wheel1, new phys2d.math.Vector2f(x-WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y))
      val joint2 = new phys2d.raw.BasicJoint(body, wheel2, new phys2d.math.Vector2f(x+WHEEL_OFFSET_X, y-100+WHEEL_OFFSET_Y))
      session.world.add(joint1)
      session.world.add(joint2)
    }
    else {
      body = new phys2d.raw.StaticBody(physShape)
      body.setPosition(x, y - 100f)
    }
    
    //body.setFriction(0.8f)
    body.setDamping(0.007f)

    session.addBody(this, body)
    session.addBody(this, wheel1)
    session.addBody(this, wheel2)

  }
  
  def gunReady = {
    gunTimer <= 0
  }
  
  def gunX = {
    x - GUN_OFFSET_X * Math.cos(angle.toRadians) - GUN_OFFSET_Y * Math.sin(angle.toRadians)
  }
  
  def gunY = {
    y - GUN_OFFSET_X * Math.sin(angle.toRadians) + GUN_OFFSET_Y * Math.cos(angle.toRadians)
  }
  
  def isAlive = {
    health > 0
  }

  def isDead = {
    !isAlive
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
    
    if (thrust != 0 && grounded) {
      body.addForce(new phys2d.math.Vector2f(SPEED*Math.cos(body.getRotation).toFloat*thrust, SPEED*Math.sin(body.getRotation).toFloat*thrust))
      body.setIsResting(false)
      //body.addForce(new phys2d.math.Vector2f(0, -95f))
    }
    else {
      //body.setFriction(0.9f)
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
      session.removeBody(body)
    }
  }
  
  def kill = {
    health = 0
  }
  
  def render(g: Graphics): Unit = {
    if (isDead) {
      return
    }
    
    //g.fillRect(10 + (playerId-1)*110, 10, health, 10)
    
    if (grounded) {
      g.setColor(color)
    } else {
      g.setColor(new Color(1f, 1f, 1f))
    }
    
    g.translate(body.getPosition.getX, body.getPosition.getY)
    g.rotate(0, 0, body.getRotation.toDegrees)
    
    g.fill(tankShape)
    
    //This draws a health bar under the tank:
    g.setColor(new Color(0f, 0f, 1f, 0.5f))
    g.fillRect(-WIDTH/2, 15, (health/100f)*WIDTH, 10)

    g.translate(GUN_OFFSET_X, GUN_OFFSET_Y)
    g.rotate(0, 0, gunAngle)
    g.scale(1, gunPower/GUN_POWER_SCALE)
    g.setColor(if (gunReady) GUN_READY_COLOR else GUN_LOADING_COLOR)
    g.fill(arrowShape)
    
    g.resetTransform
    g.translate(wheel1.getPosition.getX, wheel1.getPosition.getY)
    g.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2)
    
    g.resetTransform
    g.translate(wheel2.getPosition.getX, wheel2.getPosition.getY)
    g.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2)

    g.resetTransform
  }
  
  def serialise = {
    Operations.toByteArray(List[Short](
      x.toShort, 
      y.toShort, 
      angle.toShort, 
      gunAngle.toShort, 
      gunPower.toShort, 
      health.toShort, 
      thrust.toShort, 
      gunAngleChange.toShort, 
      gunPowerChange.toShort,
      color.r.toShort,
      color.g.toShort,
      color.b.toShort
    ))
  }
  
  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[List[short]](data)
    
    body.setPosition(values(0), values(1))
    body.setRotation(values(2).toFloat.toRadians)
    gunAngle = values(3)
    gunPower = values(4)
    health = values(5)
    thrust = values(6)
    gunAngleChange = values(7)
    gunPowerChange = values(8)
    
    color = new slick.Color(values(9).toFloat, values(10).toFloat, values(11).toFloat)
  }

}
