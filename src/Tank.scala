import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import scala.collection.mutable.HashMap

import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

import ClientTank._

abstract class Tank (val session: Session, var id: Byte) extends GameObject(session) {
  val WIDTH  = Config("tank.width").toFloat
  val HEIGHT = Config("tank.height").toFloat
  val TAPER  = Config("tank.taper").toFloat
  val BEVEL  = Config("tank.bevel").toFloat
  
  val WHEEL_RADIUS = BEVEL
  val WHEEL_OFFSET_X = WIDTH/2-BEVEL
  val WHEEL_OFFSET_Y = -BEVEL

  val BASE_WIDTH = WIDTH - 2*WHEEL_RADIUS
  val BASE_HEIGHT = BEVEL
  val BASE_OFFSET_X = 0
  val BASE_OFFSET_Y = -BASE_HEIGHT/2
  
  def color = Colors(id)
 
  val gun = new Gun(session, this)

  def shapePoints = List[slick.geom.Vector2f] (
                      new slick.geom.Vector2f(-(WIDTH/2-TAPER), -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2-TAPER, -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2, -BEVEL),
                      new slick.geom.Vector2f(-WIDTH/2, -BEVEL)
                    ).toArray

  override def shapes = {
    val bodyShapePoints = shapePoints.map((point) => new Vec2(point.x, point.y))
    val bodyShape = new PolygonDef
    bodyShapePoints.foreach(bodyShape.addVertex(_))
    bodyShape.density = 1f

    val baseShape = new PolygonDef
    baseShape.setAsBox(BASE_WIDTH, BASE_HEIGHT, new Vec2(BASE_OFFSET_X, BASE_OFFSET_Y), 0f)
    baseShape.density = 1f
    
    val wheelShape1 = new CircleDef
    wheelShape1.radius = WHEEL_RADIUS
    wheelShape1.localPosition = new Vec2(WHEEL_OFFSET_X, WHEEL_OFFSET_Y)
    wheelShape1.density = 1f
    
    val wheelShape2 = new CircleDef
    wheelShape2.radius = WHEEL_RADIUS
    wheelShape2.localPosition = new Vec2(-WHEEL_OFFSET_X, WHEEL_OFFSET_Y)
    wheelShape2.density = 1f

    List(bodyShape, baseShape, wheelShape1, wheelShape2)
  }
  
  var player: Player = null

  var health = 100

  val contactGrace = 50
  var contactTime = 0

  var jumping = false
  var airborne = false

  var maxJumpFuel = 20000
  var purchasedJumpFuel = 2000
  var jumpFuel = 0
  
  var corbomite = 0
  val maxCorbomite = Config("tank.maxCorbomite").toInt

  addShapes
  
  def fuelPercent = (jumpFuel.toFloat/maxJumpFuel) * 100

  def grounded : Boolean = contactTime > 0; true

  def angle = body.getAngle.toDegrees
  def x = body.getPosition.x
  def y = body.getPosition.y
  def velocity = body.getLinearVelocity

  def isAlive = health > 0
  def isDead = !isAlive

  def create(x: Float) = {
  }

  def update(delta: Int): Unit = {
    gun.update(delta)
  }

  def damage(d: Int, source: Projectile) {
  }
  
  def remove = {
    if (null != body) session.removeBody(body)
  }
}

