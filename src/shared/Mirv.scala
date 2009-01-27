import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random
import org.jbox2d.common._

package shared {
  object Mirv extends Item {
    override def name = "MIRV"
    override def cost = 75
    override def units = 1
    override val projectileType = ProjectileTypes.MIRV
  }
}

package server {
  import shared._
  class Mirv(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.MIRV

    val distribution = 10
    val clusterSize = 10
    val lifetime = 2000
    var timeUntilSplit = lifetime
    val rand = new Random

    def clusterProjectile: Projectile = new MirvCluster(server, tank)
    
    override def update(delta: Int) {
      super.update(delta)
      timeUntilSplit -= delta
      if (body.getLinearVelocity.y > 0 && timeUntilSplit < 0) {
        for (val i <- 0 until clusterSize) {
          val p = clusterProjectile
          p.body.setXForm(new Vec2(x, y), 0)
          p.body.setLinearVelocity(new Vec2(body.getLinearVelocity.x + (rand.nextFloat*2f - 1f) * distribution, 
                                            body.getLinearVelocity.y + (rand.nextFloat*2f - 1f) * distribution))
          p.body.setAngularVelocity((rand.nextFloat*2f - 1f) * 2f * Math.Pi.toFloat)
          server.addProjectile(p)
        }
        server.removeProjectile(this)
      }
    }
  }

  class MirvCluster(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.MIRV_CLUSTER
  }
}
