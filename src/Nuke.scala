import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

class Nuke(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 5f
  override val EXPLOSION_RADIUS = 40f
  override val damage = 10
  override val projectileType = ProjectileTypes.NUKE
}
