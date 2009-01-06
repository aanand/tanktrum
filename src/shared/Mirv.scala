import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random
import org.jbox2d.common._

package shared {
  object MirvItem extends Item {
    override def name = "MIRV"
    override def cost = 75
    override def units = 1
    override val projectileType = ProjectileTypes.MIRV
  }

  trait Mirv extends Projectile {
    override lazy val radius = 0.8f
    override val projectileType = ProjectileTypes.MIRV
    override val explosionRadius = 2.6f

    override def imagePath = Config("projectile.mirv.imagePath")
    override def imageWidth = Config("projectile.mirv.imageWidth").toInt
    override def round = Config("projectile.mirv.round").toBoolean

    val distribution = 10
    val clusterSize = 10
    val lifetime = 2000
    var timeUntilSplit = lifetime
    val rand = new Random
  }
  
  trait MirvCluster extends Projectile {
    override lazy val radius = 0.4f
    override val damage = 3
    override val explosionRadius = 2.6f
    override val projectileType = ProjectileTypes.MIRV_CLUSTER
  }
}

package server {
  class Mirv(server: Server, tank: Tank) extends Projectile(server, tank) with shared.Mirv {
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
          server.addProjectile(p)
        }
        server.removeProjectile(this)
      }
    }
  }

  class MirvCluster(server: Server, tank: Tank) extends Projectile(server, tank) with shared.MirvCluster
}

package client {
  class Mirv extends Projectile with shared.Mirv
  class MirvCluster extends Projectile with shared.MirvCluster
}



