import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random

package shared {
  object DeathsHead extends Item {
    override def name = "Death's Head"
    override def cost = 350
    override def units = 1
    override val projectileType = ProjectileTypes.DEATHS_HEAD
  }
}

package server {
  import shared._
  class DeathsHead(server: Server, tank: Tank) extends Mirv(server, tank) {
    override def clusterProjectile = new DeathsHeadCluster(server, tank)
    override val projectileType = ProjectileTypes.DEATHS_HEAD
  }

  class DeathsHeadCluster(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.DEATHS_HEAD_CLUSTER
  }
}
