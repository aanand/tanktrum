import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object CorbomiteItem extends Item {
  override def name = "Corbomite"
  override def cost = 50
  override def units = 5
}

class Corbomite(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 2f
  override val mass = 0.4f
  override val explosionRadius = 10f
  override val projectileType = ProjectileTypes.CORBOMITE
}
