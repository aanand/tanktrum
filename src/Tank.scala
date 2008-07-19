import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import net.phys2d.raw.shapes._
import net.phys2d.math._
import net.phys2d

import sbinary.Instances._
import sbinary.Operations

class Tank (container: GameContainer) {
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
  var gunAngle = 0
  var gunPower = 0
  var gunTimer = 0f

  var health = 100

  def create(x: Float, color: Color) = {
    //y = container.ground.heightAt(x)
    //angle = container.ground.normalAt(x)
    this.color = color
    //body.setPosition(x, y)
    //container.addBody(this, body)
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
  
  def update(container: GameContainer, delta: Int): Unit = {
    if (isDead) {
      return
    }
    
    if (thrust != 0) {
      //x += thrust * SPEED * delta / 1000.0f * Math.cos(angle.toRadians)
      reposition
    }
    
    if (gunAngleChange != 0) {
      //gunAngle = (gunAngle + gunAngleChange * GUN_ANGLE_SPEED * delta / 1000.0f).constrain(GUN_ANGLE_RANGE)
    }
    
    if (gunPowerChange != 0)
      //gunPower = (gunPower + gunPowerChange * GUN_POWER_SPEED * delta / 1000.0).constrain(GUN_POWER_RANGE)
    
    if (!gunReady) {
      gunTimer -= delta / 1000.0f
    }
  }
  
  //TODO: Got to here.

  def reposition = {
    /*var xLeft  = x - WIDTH/2 * Math.cos(angle.toRadians)
    var xRight = x + WIDTH/2 * Math.cos(angle.toRadians)
    
    yLeft   = container.ground.heightAt(xLeft)
    yRight  = container.ground.heightAt(xRight)
    
    yAvg    = (yLeft + yRight) / 2.0
    yMiddle = container.ground.heightAt(x)

    y = [yAvg, yMiddle].min
    
    angle = Math.atan((yRight - yLeft) / (xRight - xLeft)).to_degrees
    
    body.setPosition(x, y)*/
  }
  
  def fire: Unit = {
    if (isDead) return
    
    if (gunReady) {
      //container.addProjectile(self, gunX, gunY, angle+gunAngle, gunPower, true)
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
    //container.removeBody(body)
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
    //g.setColor(gunReady ? GUN_READY_COLOR : GUN_LOADING_COLOR)
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
