import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations
import java.util.Random;

object DeathsHeadItem extends Item {
  override def name = "Death's Head"
  override def cost = 350
  override def units = 1
  override val projectileType = ProjectileTypes.DEATHS_HEAD
}

class DeathsHead(session: Session, tank: Tank) extends MIRV(session, tank) {
  override val radius = 5f
  override val explosionRadius = 60f
  override val projectileType = ProjectileTypes.DEATHS_HEAD

  override def clusterProjectile = new Nuke(session, tank)
}
