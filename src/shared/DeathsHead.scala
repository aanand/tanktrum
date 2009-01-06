import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import java.util.Random

package shared {
  object DeathsHeadItem extends Item {
    override def name = "Death's Head"
    override def cost = 350
    override def units = 1
    override val projectileType = ProjectileTypes.DEATHS_HEAD
  }

  trait DeathsHead extends Mirv {
    override val radius = 1.2f
    override val explosionRadius = 6f
    override val projectileType = ProjectileTypes.DEATHS_HEAD
    override val clusterSize = 8

    override def imagePath = Config("projectile.deathsHead.imagePath")
    override def imageWidth = Config("projectile.deathsHead.imageWidth").toInt
    override def round = Config("projectile.deathsHead.round").toBoolean
  }

  trait DeathsHeadCluster extends Projectile {
    override val radius = 0.8f
    override val damage = 5
    override val explosionRadius = 6f
    override val explosionDamageFactor = 1.2f
    override val projectileType = ProjectileTypes.DEATHS_HEAD_CLUSTER

    override def imagePath = Config("projectile.deathsHeadCluster.imagePath")
    override def imageWidth = Config("projectile.deathsHeadCluster.imageWidth").toInt
    override def round = Config("projectile.deathsHeadCluster.round").toBoolean
  }  
}

package server {
  class DeathsHead(server: Server, tank: Tank) extends Mirv(server, tank) with shared.DeathsHead {
    override def clusterProjectile = new DeathsHeadCluster(server, tank)
  }

  class DeathsHeadCluster(server: Server, tank: Tank) extends Projectile(server, tank) with shared.DeathsHeadCluster
}

package client {
  class DeathsHead extends Mirv with shared.DeathsHead
  class DeathsHeadCluster extends Projectile with shared.DeathsHeadCluster
}
