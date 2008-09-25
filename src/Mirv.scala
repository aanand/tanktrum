import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random;
import org.jbox2d.common._

object MirvItem extends Item {
  override def name = "MIRV"
  override def cost = 75
  override def units = 1
  override val projectileType = ProjectileTypes.MIRV
}

class Mirv(session: Session, tank: Tank) extends Projectile(session, tank) {
  val rand = new Random
  val DISTRIBUTION = 50
  val clusterSize = 10
  var timeUntilSplit = 2000
  override val projectileType = ProjectileTypes.MIRV
  //override val radius = 4f
  override val explosionRadius = 13f

  def clusterProjectile: Projectile = new MirvCluster(session, tank)
  
  override def update(delta: Int) {
    super.update(delta)
    timeUntilSplit -= delta
    if (session.isInstanceOf[Server]) {
      if (body.getLinearVelocity.y > 0 && timeUntilSplit < 0) {
        for (val i <- 0 until clusterSize) {
          val p = clusterProjectile
          p.body.setXForm(new Vec2(x, y), 0)
          p.body.setLinearVelocity(new Vec2(body.getLinearVelocity.x + (rand.nextFloat*2f - 1f) * DISTRIBUTION, 
                                            body.getLinearVelocity.y + (rand.nextFloat*2f - 1f) * DISTRIBUTION))
          session.addProjectile(p)
        }
        session.removeProjectile(this)
      }
    }
  }
}

class MirvCluster(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 2f
  override val damage = 3
  override val explosionRadius = 13f
  override val projectileType = ProjectileTypes.MIRV_CLUSTER
}
