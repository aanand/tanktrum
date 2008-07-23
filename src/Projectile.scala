import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Projectile(session : Session, tank : Tank, val body : phys2d.raw.Body, radius : Float) extends Collider {
  val COLOR = new slick.Color(1.0f, 1.0f, 1.0f)
  val EXPLOSION_RADIUS = 20f

  var destroy = false

  def x = body.getPosition.getX
  def y = body.getPosition.getY
  
  def update(container : slick.GameContainer, delta : Int) {
    if (x < 0 || x > container.getWidth || y > container.getHeight || destroy) {
      session.removeProjectile(this)
    }
  }
  
  def render(container : slick.GameContainer, g : slick.Graphics) {
    g.setColor(COLOR)
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }
  
  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    if (obj.isInstanceOf[Projectile]) {
      return
    }

    destroy = true
    session.addExplosion(x, y, EXPLOSION_RADIUS)

    if (session.isInstanceOf[Server]) {
      session.ground.deform(x.toInt, y.toInt, EXPLOSION_RADIUS.toInt)
    }
    
    if (obj.isInstanceOf[Tank] && session.isInstanceOf[Server]) {
      val tank = obj.asInstanceOf[Tank]
      val server = session.asInstanceOf[Server]
      
      tank.damage(20)
      server.broadcastDamageUpdate(tank, 20)
    }
  }

  def serialise = {
    Operations.toByteArray(List[Float](
      x,
      y,
      body.getVelocity.getX,
      body.getVelocity.getY
    ))
  }

  def loadFrom(data: Array[byte]) =  {
    val values = Operations.fromByteArray[List[Float]](data)
    body.setPosition(values(0), values(1))
    body.adjustVelocity(new phys2d.math.Vector2f(values(2), values(3)))
  }
}
