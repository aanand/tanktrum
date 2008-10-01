import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

object CorbomiteItem extends Item {
  override def name = "Corbomite"
  override def cost = 50
  override def units = 8
}

class Corbomite(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 0.4f
  override val explosionRadius = 2f
  override val projectileType = ProjectileTypes.CORBOMITE
}
