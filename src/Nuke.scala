import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

object NukeItem extends Item {
  override def name = "Nuke"
  override def cost = 75
  override def units = 1
  override val projectileType = ProjectileTypes.NUKE
}

class Nuke(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 5f
  override val mass = 3f
  override val explosionRadius = 40f
  override val damage = 10
  override val reloadTime = 5f
  override val projectileType = ProjectileTypes.NUKE
}
