import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Roller(session : Session, tank : Tank) extends Projectile(session, tank) {
  override val projectileType = ProjectileTypes.ROLLER

  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    if (!obj.isInstanceOf[Tank] || destroy) {
      return
    }
    super.collide(obj, event) //heh
  }
}
