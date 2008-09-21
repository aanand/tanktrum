import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations
import java.util.Random;

object MIRVItem extends Item {
  override def name = "MIRV"
  override def cost = 75
  override def units = 1
  override val projectileType = ProjectileTypes.MIRV
}

class MIRV(session: Session, tank: Tank) extends Projectile(session, tank) {
  val rand = new Random
  val DISTRIBUTION = 50
  val clusterSize = 10
  var timeUntilSplit = 2000
  override val projectileType = ProjectileTypes.MIRV
  override val radius = 4f
  override val explosionRadius = 13f

  def clusterProjectile: Projectile = new MIRVCluster(session, tank)

  override def update(delta: Int) {
    super.update(delta)
    timeUntilSplit -= delta
    if (session.isInstanceOf[Server]) {
      if (body.getVelocity.getY > 0 && timeUntilSplit < 0) {
        for (val i <- 0 until clusterSize) {
          val p = clusterProjectile
          p.body.setPosition(x, y)
          p.body.adjustVelocity(new phys2d.math.Vector2f(body.getVelocity.getX + (rand.nextFloat*2f - 1f) * DISTRIBUTION, 
                                                         body.getVelocity.getY + (rand.nextFloat*2f - 1f) * DISTRIBUTION))
          session.addProjectile(p)
        }
        session.removeProjectile(this)
      }
    }
  }
}

class MIRVCluster(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 2f
  override val damage = 3
  override val explosionRadius = 13f
  override val projectileType = ProjectileTypes.MIRV_CLUSTER
}
