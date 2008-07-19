import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import net.phys2d.raw.shapes._
import net.phys2d.math._
import net.phys2d

import sbinary.Instances._
import sbinary.Operations

class Tank (game: Session) {
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

  var x: Float = _
  var y: Float = _
  var angle: Float = _
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
    y = game.getGround.heightAt(x)
    angle = game.getGround.normalAt(x)
    this.color = color
    body.setPosition(x, y)
    game.addBody(this, body)
    reposition
  }
  
  def gunReady = {
    gunTimer <= 0
  }
  
  //TODO: Do these:
  def gunX = {
    //x - GUN_OFFSET_X * Math.cos(angle.toRadians) - GUN_OFFSET_Y * Math.sin(angle.toRadians)
  }
  
  def gunY = {
    //y - GUN_OFFSET_X * Math.sin(angle.toRadians) + GUN_OFFSET_Y * Math.cos(angle.toRadians)
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
      x = x + thrust * SPEED * delta / 1000.0f * Math.cos(Math.toRadians(angle)).toFloat
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
    
    val yLeft   = game.getGround.heightAt(xLeft.toFloat)
    val yRight  = game.getGround.heightAt(xRight.toFloat)
    
    val yAvg    = (yLeft + yRight) / 2.0
    val yMiddle = game.getGround.heightAt(x)

    y = Math.min(yAvg.toFloat, yMiddle.toFloat)
    
    angle = Math.toDegrees(Math.atan((yRight - yLeft) / (xRight - xLeft))).toFloat
    
    body.setPosition(x, y)
  }
  
  def fire() {
    if (isDead) return
    
    if (gunReady) {
      //game.addProjectile(self, gunX, gunY, angle+gunAngle, gunPower, true)
      gunTimer = GUN_RELOAD_TIME
    }
  }
  
  def damage(d: Int) = {
    health -= d
    
    if (d < 0) {
      die
    }
  }
  
  def die = {
    //game.removeBody(body)
  }
  
  def render(g: Graphics): Unit = {
    if (isDead) {
      return
    }
    
    g.setColor(color)
    
    //g.fillRect(10 + (playerId-1)*110, 10, health, 10)
    
    g.translate(x, y)
    g.rotate(0, 0, angle)
    
    g.fill(tankShape)
    
    g.translate(GUN_OFFSET_X, GUN_OFFSET_Y)
    g.rotate(0, 0, gunAngle)
    g.scale(gunPower/GUN_POWER_SCALE, gunPower/GUN_POWER_SCALE)
    g.setColor(if (gunReady) GUN_READY_COLOR else GUN_LOADING_COLOR)
    g.fill(arrowShape)
      
    g.resetTransform
  }
  
  /*
  def collide (obj: Int, event: Int)
  end
  */

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
    ).toArray)
  }
  
  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[Array[short]](data)
    x = values(0)
    y = values(1)
    angle = values(2)
    gunAngle = values(3)
    gunPower = values(4)
    health = values(5)
    thrust = values(6)
    gunAngleChange = values(7)
    gunPowerChange = values(8)
    color.r = values(9)
    color.g = values(10)
    color.b = values(11)
  }
}
