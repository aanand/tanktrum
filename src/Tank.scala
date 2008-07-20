import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import net.phys2d.raw.shapes._
import net.phys2d.math._
import net.phys2d

import sbinary.Instances._
import sbinary.Operations

class Tank (session: Session) extends Collider {
  val WIDTH = 40f
  val HEIGHT = 20f
  val TAPER = 5f

  val SPEED = 10 // pixels/second
  
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
                      new slick.geom.Vector2f(WIDTH/2, 0),
                      new slick.geom.Vector2f(-WIDTH/2, 0)
                    ).toArray

  val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY())).toArray
  val tankShape = new slick.geom.Polygon(drawShapePoints)
  val arrowShape = new slick.geom.Polygon(List[Float](-5, 0, -5, -50, -10, -50, 0, -60, 10, -50, 5, -50, 5, 0).toArray)

  val physShapePoints = shapePoints.map((point) => new phys2d.math.Vector2f(point.x, point.y))
  val physShape = new phys2d.raw.shapes.Polygon(physShapePoints.toArray)
  val body = new phys2d.raw.StaticBody(physShape)

  var x: Double = _
  var y: Double = _
  var angle: Double = _
  var color: Color = _

  var thrust = 0
  var gunAngleChange = 0
  var gunPowerChange = 0

  var gunAngle = 0f
  var gunPower = 200f
  var gunTimer = 0f

  var health = 100

  def create(x: Float, color: Color) = {
    this.x = x
    y = session.getGround.heightAt(x)
    angle = session.getGround.normalAt(x)
    this.color = color
    body.setPosition(x.toFloat, y.toFloat)
    session.addBody(this, body)
    reposition
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
    
    if (thrust != 0) {
      x = x + thrust * SPEED * delta / 1000.0f * Math.cos(Math.toRadians(angle))
      reposition
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
  
  def reposition = {
    val xLeft  = x - WIDTH/2 * Math.cos(angle.toRadians)
    val xRight = x + WIDTH/2 * Math.cos(angle.toRadians)
    
    val yLeft   = session.getGround.heightAt(xLeft)
    val yRight  = session.getGround.heightAt(xRight)
    
    val yAvg    = (yLeft + yRight) / 2.0
    val yMiddle = session.getGround.heightAt(x)

    y = Math.min(yAvg, yMiddle)
    
    angle = Math.toDegrees(Math.atan((yRight - yLeft) / (xRight - xLeft)))
    
    body.setPosition(x.toFloat, y.toFloat)
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
      die
    }
  }
  
  def die = {
    session.removeBody(body)
  }
  
  def render(g: Graphics): Unit = {
    if (isDead) {
      return
    }
    
    
    //g.fillRect(10 + (playerId-1)*110, 10, health, 10)
    
    g.setColor(color)
    g.translate(x.toFloat, y.toFloat)
    g.rotate(0, 0, angle.toFloat)
    
    g.fill(tankShape)
    
    /*
    This would draw a health bar under the tank:
    g.setColor(new Color(0f, 0f, 1f, 0.5f))
    g.fillRect(-WIDTH/2, 15, (100f/health)*WIDTH, 10)
    */

    g.translate(GUN_OFFSET_X, GUN_OFFSET_Y)
    g.rotate(0, 0, gunAngle)
    g.scale(1, gunPower/GUN_POWER_SCALE)
    g.setColor(if (gunReady) GUN_READY_COLOR else GUN_LOADING_COLOR)
    g.fill(arrowShape)
    
    //draw healthbar
      
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
      color.b.toShort,
    ))
  }
  
  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[List[short]](data)
    
    x = values(0)
    y = values(1)
    angle = values(2)
    gunAngle = values(3)
    gunPower = values(4)
    health = values(5)
    thrust = values(6)
    gunAngleChange = values(7)
    gunPowerChange = values(8)
    
    color = new slick.Color(values(9).toFloat, values(10).toFloat, values(11).toFloat)
  }
}
