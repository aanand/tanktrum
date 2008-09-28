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
import org.jbox2d.collision

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
  val friction = Config("tank.friction").toFloat
  
  def color = Colors(id)
 
  val gun = new Gun(session, this)
  
  var player: Player = null

  var health = 100f

  val contactGrace = 50
  var contactTime = 0

  var jumping = false
  var airborne = false

  var maxJumpFuel = 20000
  var purchasedJumpFuel = 2000
  var jumpFuel = 0
  
  var corbomite = 0
  val maxCorbomite = Config("tank.maxCorbomite").toInt

  var topShape: collision.Shape = _
  var baseShape: collision.Shape = _
  var wheelShape1: collision.Shape = _
  var wheelShape2: collision.Shape = _

  def shapePoints = List[slick.geom.Vector2f] (
                      new slick.geom.Vector2f(-(WIDTH/2-TAPER), -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2-TAPER, -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2, -BEVEL),
                      new slick.geom.Vector2f(-WIDTH/2, -BEVEL)
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
    baseShapeDef.density = 1f
    baseShapeDef.restitution = 0f
    baseShapeDef.friction = friction
    
    val wheelShapeDef1 = new CircleDef
    wheelShapeDef1.radius = WHEEL_RADIUS
    wheelShapeDef1.localPosition = new Vec2(WHEEL_OFFSET_X, WHEEL_OFFSET_Y)
    wheelShapeDef1.density = 1f
    wheelShapeDef1.restitution = 0f
    wheelShapeDef1.friction = friction
    
    val wheelShapeDef2 = new CircleDef
    wheelShapeDef2.radius = WHEEL_RADIUS
    wheelShapeDef2.localPosition = new Vec2(-WHEEL_OFFSET_X, WHEEL_OFFSET_Y)
    wheelShapeDef2.density = 1f
    wheelShapeDef2.restitution = 0f
    wheelShapeDef2.friction = friction

    List(bodyShapeDef, baseShapeDef, wheelShapeDef1, wheelShapeDef2)
  }

  override def addShapes = {
    val shapesList = shapes
    topShape = body.createShape(shapesList(0))
    baseShape = body.createShape(shapesList(1))
    wheelShape1 = body.createShape(shapesList(2))
    wheelShape2 = body.createShape(shapesList(3))
  }

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

  def damage(d: Float, source: Projectile) {
  }
  
  def remove = {
    if (null != body) session.removeBody(body)
  }
}

