import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Projectile(session: Session, val tank: Tank) extends Collider {
  val color = new slick.Color(1.0f, 1.0f, 1.0f)
  val explosion_radius = 20f
  val radius = 3f
  val damage = 5
  val reloadTime = 4f
  val shape = new phys2d.raw.shapes.Circle(radius)
  
  var body: phys2d.raw.Body = _

  if (session.isInstanceOf[Server]) {
    body = new phys2d.raw.Body(shape, 1.0f)
  }
  else {
    body = new phys2d.raw.StaticBody(shape)
  }

  body.addExcludedBody(session.ground.body)

  val projectileType = ProjectileTypes.PROJECTILE

  var destroy = false

  session.addBody(this, body)
  
  def x = body.getPosition.getX
  def y = body.getPosition.getY
  
  def update(delta : Int) {
    if (x < 0 || x > Main.WIDTH || y > Main.HEIGHT || destroy) {
      session.removeProjectile(this)
    }
    if (y + radius > session.ground.heightAt(x)) {
      collide(session.ground, null)
    }
  }
  
  def render(g : slick.Graphics) {
    g.setColor(color)
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }
  
  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    if ((obj.isInstanceOf[Projectile] && !this.isInstanceOf[Roller]) || destroy) {
      return
    }

    destroy = true

    if (session.isInstanceOf[Server]) {
      session.addExplosion(x, y, explosion_radius, this)
      session.ground.deform(x.toInt, y.toInt, explosion_radius.toInt)
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
        //TODO: Use a tank id to track which tank this projectile came from.
      p = ProjectileTypes.newProjectile(session, null, ProjectileTypes(projectileType))
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
  val PROJECTILE, NUKE, ROLLER, MIRV, MACHINE_GUN = Value

  def newProjectile(session: Session, tank: Tank, projectileType: Value) : Projectile = {
    projectileType match {
      case PROJECTILE => { new Projectile(session, tank) }
      case NUKE => { new Nuke(session, tank) }
      case ROLLER => { new Roller(session, tank) }
      case MIRV => { new MIRV(session, tank) }
      case MACHINE_GUN => { new MachineGun(session, tank) }
    }
  }

  def render(g: slick.Graphics, value: Value) {
    g.setColor(new slick.Color(1f, 1f, 1f))
    value match {
      case PROJECTILE => {
        g.fillOval(-3, -3, 6, 6)
      }
      case NUKE => {
        g.fillOval(-6, -6, 12, 12)
      }
      case ROLLER => {
        g.fillOval(-3, -3, 6, 6)
        g.fillRect(-7, 3, 14, 4)
      }
      case MIRV => {
        g.fillOval(-2, -2, 4, 4)
        g.fillOval(2, 2, 4, 4)
        g.fillOval(-2, 2, 4, 4)
        g.fillOval(2, -2, 4, 4)
      }
      case MACHINE_GUN => {
        g.fillRect(-2, -2, 4, 8)
      }
    }
  }
}
