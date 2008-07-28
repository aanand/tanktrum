import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Projectile(session : Session, tank : Tank) extends Collider {
  val COLOR = new slick.Color(1.0f, 1.0f, 1.0f)
  val EXPLOSION_RADIUS = 20f
  val radius = 3f
  val damage = 20
  val shape = new phys2d.raw.shapes.Circle(radius)
  val body = new phys2d.raw.Body(shape, 1.0f)
  val projectileType = ProjectileTypes.PROJECTILE

  var destroy = false

  session.addBody(this, body)
  
  def x = body.getPosition.getX
  def y = body.getPosition.getY
  
  def update(delta : Int) {
    if (x < 0 || x > Main.WIDTH || y > Main.HEIGHT || destroy) {
      session.removeProjectile(this)
    }
  }
  
  def render(g : slick.Graphics) {
    g.setColor(COLOR)
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }
  
  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    if (obj.isInstanceOf[Projectile]) {
      return
    }

    destroy = true
    session.addExplosion(x, y, EXPLOSION_RADIUS)

    //TODO: This would possibly play a sound, but causes a link error
    //currently.
    /*
    val file = "home/norgg/svn.norgg.org/deathtank/media/dtz-yeah.ogg"
    val audio = new slick.openal.OpenALStreamPlayer(44000, file)
    audio.play(true)
    */

    if (session.isInstanceOf[Server]) {
      session.ground.deform(x.toInt, y.toInt, EXPLOSION_RADIUS.toInt)
    }
    
    if (obj.isInstanceOf[Tank] && session.isInstanceOf[Server]) {
      val tank = obj.asInstanceOf[Tank]
      val server = session.asInstanceOf[Server]
      
      tank.damage(damage)
      server.broadcastDamageUpdate(tank, damage)
    }
  }

  def serialise = {
    Operations.toByteArray((
      x,
      y,
      body.getVelocity.getX,
      body.getVelocity.getY,
      projectileType.id.toByte
    ))
  }

  def loadFrom(data: Array[byte]) =  {
    val values = Operations.fromByteArray[List[Float]](data)
    body.setPosition(values(0), values(1))
    body.adjustVelocity(new phys2d.math.Vector2f(values(2), values(3)))
  }
}

object ProjectileLoader {
  def loadProjectile(data: Array[byte], session: Session) = {
    val (x, y, xVel, yVel, projectileType) = Operations.fromByteArray[(Float, Float, Float, Float, Byte)](data)
    var p: Projectile = null
    ProjectileTypes.apply(projectileType) match {
      //TODO: Use a tank id to track which tank this projectile came from.
      case ProjectileTypes.PROJECTILE => { p = new Projectile(session, null) }
      case ProjectileTypes.NUKE => { p = new Nuke(session, null) }
    }
    p.body.setPosition(x, y)
    p.body.adjustVelocity(new phys2d.math.Vector2f(xVel, yVel))
    p
  }
}

object ProjectileTypes extends Enumeration {
  val PROJECTILE, NUKE = Value
}
