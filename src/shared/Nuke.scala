import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

package shared {
  object Nuke extends Item {
    override def name = "Nuke"
    override def cost = 75
    override def units = 1
    override val projectileType = ProjectileTypes.NUKE
  }
}

package server {
  import shared._
  class Nuke(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.NUKE
  }
}
