import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object MachineGunItem extends Item {
  override def name = "Machine Gun"
  override def cost = 50
  override def units = 30
  override val projectileType = ProjectileTypes.MACHINE_GUN
}

class MachineGun(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 2f
  override val EXPLOSION_RADIUS = 4f
  override val damage = 3
  override val reloadTime = 0.3f
  override val projectileType = ProjectileTypes.MACHINE_GUN
}
