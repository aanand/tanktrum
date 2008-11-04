import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random
import org.jbox2d.common._

object MirvItem extends Item {
  override def name = "MIRV"
  override def cost = 75
  override def units = 1
  override val projectileType = ProjectileTypes.MIRV
}

class Mirv(session: Session, tank: Tank) extends Projectile(session, tank) {
  override lazy val radius = 0.8f
  override val projectileType = ProjectileTypes.MIRV
  override val explosionRadius = 2.6f
  val distribution = 10
  val clusterSize = 10
  val lifetime = 2000
  var timeUntilSplit = lifetime
  val rand = new Random

  def clusterProjectile: Projectile = new MirvCluster(session, tank)
  
  override def update(delta: Int) {
    super.update(delta)
    timeUntilSplit -= delta
    if (session.isInstanceOf[Server]) {
      if (body.getLinearVelocity.y > 0 && timeUntilSplit < 0) {
        for (val i <- 0 until clusterSize) {
          val p = clusterProjectile
          p.body.setXForm(new Vec2(x, y), 0)
          p.body.setLinearVelocity(new Vec2(body.getLinearVelocity.x + (rand.nextFloat*2f - 1f) * distribution, 
                                            body.getLinearVelocity.y + (rand.nextFloat*2f - 1f) * distribution))
          session.addProjectile(p)
        }
        session.removeProjectile(this)
      }
    }
  }

  override def color = {
    val lifeRatio = timeUntilSplit.toFloat/lifetime
    new slick.Color(1-lifeRatio, 0f, 0f)
  }
}

class MirvCluster(session: Session, tank: Tank) extends Projectile(session, tank) {
  override lazy val radius = 0.4f
  override val damage = 3
  override val explosionRadius = 2.6f
  override val projectileType = ProjectileTypes.MIRV_CLUSTER
}
