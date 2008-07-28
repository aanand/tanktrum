import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Roller(session : Session, tank : Tank) extends Projectile(session, tank) {
  override val projectileType = ProjectileTypes.ROLLER

  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    if (!obj.isInstanceOf[Tank]) {
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
      
      tank.damage(damage)
      server.broadcastDamageUpdate(tank, damage)
    }
  }
}
