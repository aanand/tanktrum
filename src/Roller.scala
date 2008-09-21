import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object RollerItem extends Item { 
  override def name = "Roller"
  override def cost = 100
  override def units = 5
  override val projectileType = ProjectileTypes.ROLLER
}

class Roller(session : Session, tank : Tank) extends Projectile(session, tank) {
  override val projectileType = ProjectileTypes.ROLLER
  override val color = new slick.Color(0.4f, 0.6f, 0f)
  override val damage = 10
  override val explosionRadius = 25f

  body.removeExcludedBody(session.ground.body)
  body.setRestitution(1f)

  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    if (y - radius > session.ground.heightAt(x) || //It's below the ground.
        obj.isInstanceOf[Tank] || //Or it's hit a tank.
        obj.isInstanceOf[Projectile]) { //Or it's hit a projectile.)
        explode(obj)
    }
  }
}
