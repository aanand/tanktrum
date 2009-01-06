import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

package shared {
  object NukeItem extends Item {
    override def name = "Nuke"
    override def cost = 75
    override def units = 1
    override val projectileType = ProjectileTypes.NUKE
  }

  trait Nuke extends Projectile {
    override val radius = 1f
    override val explosionRadius = 8f
    override val explosionDamageFactor = 1.5f
    override val damage = 10
    override val reloadTime = 5f
    override val projectileType = ProjectileTypes.NUKE

    override def imagePath = Config("projectile.nuke.imagePath")
    override def imageWidth = Config("projectile.nuke.imageWidth").toInt
    override def round = Config("projectile.nuke.round").toBoolean
  }
}

package client {
  class Nuke extends Projectile with shared.Nuke
}

package server {
  class Nuke(server: Server, tank: Tank) extends Projectile(server, tank) with shared.Nuke
}

