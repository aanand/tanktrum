import org.newdawn.slick.geom._
import org.newdawn.slick._
import org.newdawn.slick

import net.phys2d.raw.shapes._
import net.phys2d.math._
import net.phys2d

import scala.collection.mutable.HashMap

import sbinary.Instances._
import sbinary.Operations

import ClientTank._

abstract class Tank (val session: Session, var id: Byte) extends Collider {
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

  val shapePoints = List[slick.geom.Vector2f] (
                      new slick.geom.Vector2f(-(WIDTH/2-TAPER), -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2-TAPER, -HEIGHT),
                      new slick.geom.Vector2f(WIDTH/2, -BEVEL),
                      new slick.geom.Vector2f(-WIDTH/2, -BEVEL)
                    ).toArray
  
  val physShapePoints = shapePoints.map((point) => new phys2d.math.Vector2f(point.x, point.y))
  val physShape = new phys2d.raw.shapes.Polygon(physShapePoints.toArray)
  val baseShape = new phys2d.raw.shapes.Box(BASE_WIDTH, BASE_HEIGHT);
  val wheelShape = new phys2d.raw.shapes.Circle(WHEEL_RADIUS)
  
  var player: Player = null

  var body: phys2d.raw.Body = _
  var wheel1: phys2d.raw.Body = _
  var wheel2: phys2d.raw.Body = _
  var base: phys2d.raw.Body = _

  var health = 100

  val contactGrace = 50
  var contactTime = 0

  var jumping = false
  var airborne = false

  var maxJumpFuel = 20000
  var purchasedJumpFuel = 2000
  var jumpFuel = 0
  
  var corbomite = 0
  
  def fuelPercent = (jumpFuel.toFloat/maxJumpFuel) * 100

  def grounded : Boolean = contactTime > 0; true

  def angle = body.getRotation.toDegrees
  def x = body.getPosition.getX
  def y = body.getPosition.getY
  def velocity = body.getVelocity

  def isAlive = health > 0
  def isDead = !isAlive

  def create(x: Float) = {
    session.addBody(this, body)
  }

  def update(delta: Int): Unit = {
    gun.update(delta)
  }

  def damage(d: Int, source: Projectile) {
  }
  
  def remove = {
    if (null != body) session.removeBody(body)
    if (null != wheel1) session.removeBody(wheel1)
    if (null != wheel2) session.removeBody(wheel2)
    if (null != base) session.removeBody(base)
  }
}

