import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object MachineGunItem extends Item {
  override def name = "Machine Gun"
  override def cost = 100
  override def units = 50
  override val projectileType = ProjectileTypes.MACHINE_GUN
}

class MachineGun(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 2f
  override val explosionRadius = 4f
  override val damage = 3
  override val reloadTime = 0.4f
  override val projectileType = ProjectileTypes.MACHINE_GUN
}
