import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Projectile(session: Session, val tank: Tank) extends Collider {
  val COLOR = new slick.Color(1.0f, 1.0f, 1.0f)
  val EXPLOSION_RADIUS = 20f
  val radius = 3f
  val damage = 5
  val shape = new phys2d.raw.shapes.Circle(radius)
  val body: phys2d.raw.Body = new phys2d.raw.Body(shape, 1.0f)

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
    if (obj.isInstanceOf[Projectile] || destroy) {
      return
    }

    destroy = true

    if (session.isInstanceOf[Server]) {
      session.addExplosion(x, y, EXPLOSION_RADIUS, this)
      session.ground.deform(x.toInt, y.toInt, EXPLOSION_RADIUS.toInt)
    }
    
    if (obj.isInstanceOf[Tank] && session.isInstanceOf[Server]) {
      val hitTank = obj.asInstanceOf[Tank]
      val server = session.asInstanceOf[Server]
      
      hitTank.damage(damage)
      if (tank != null && tank.player != null) {
        if (tank == hitTank) {
          tank.player.score -= damage
          tank.player.money -= damage
        }
        else {
          tank.player.score += damage
          tank.player.money += damage
        }
      }
      server.broadcastDamageUpdate(hitTank, damage)
    }
  }

  def serialise = {
    Operations.toByteArray((
      x,
      y,
      body.getVelocity.getX,
      body.getVelocity.getY,
      body.getRotation,
      body.getAngularVelocity,
      projectileType.id.toByte
    ))
  }
}

object ProjectileLoader {
  def loadProjectile(oldProjectile: Projectile, data: Array[byte], session: Session) = {
    val (x, y, xVel, yVel, rot, angVel, projectileType) = Operations.fromByteArray[(Float, Float, Float, Float, Float, Float, Byte)](data)
    var p: Projectile = null
    if (null != oldProjectile && oldProjectile.projectileType.id == projectileType) {
      p = oldProjectile
    }
    else {
      if (oldProjectile != null) {
        session.removeProjectile(oldProjectile)
      }
      ProjectileTypes.apply(projectileType) match {
        //TODO: Use a tank id to track which tank this projectile came from.
        case ProjectileTypes.PROJECTILE => { p = new Projectile(session, null) }
        case ProjectileTypes.NUKE => { p = new Nuke(session, null) }
        case ProjectileTypes.ROLLER => { p = new Roller(session, null) }
      }
    }
    p.body.setPosition(x, y)
    val vel = new phys2d.math.Vector2f(xVel, yVel)
    vel.sub(p.body.getVelocity)
    p.body.adjustVelocity(vel)
    p.body.setRotation(rot)
    p.body.adjustAngularVelocity(angVel - p.body.getAngularVelocity)
    p
  }
}

object ProjectileTypes extends Enumeration {
  val PROJECTILE, NUKE, ROLLER = Value
}
