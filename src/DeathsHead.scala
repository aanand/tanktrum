import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random;

object DeathsHeadItem extends Item {
  override def name = "Death's Head"
  override def cost = 350
  override def units = 1
  override val projectileType = ProjectileTypes.DEATHS_HEAD
}

class DeathsHead(session: Session, tank: Tank) extends Mirv(session, tank) {
  override val radius = 1.2f
  override val explosionRadius = 6f
  override val projectileType = ProjectileTypes.DEATHS_HEAD
  override val clusterSize = 8

  override def clusterProjectile = new DeathsHeadCluster(session, tank)
}

class DeathsHeadCluster(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 0.8f
  override val damage = 5
  override val explosionRadius = 6f
  override val explosionDamageFactor = 1.2f
  override val projectileType = ProjectileTypes.DEATHS_HEAD_CLUSTER
}
