package shared {
  object Corbomite extends Item {
    override def name = "Corbomite"
    override def cost = 50
    override def units = 8
  }
}

package server {
  import shared._
  class Corbomite(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.CORBOMITE
  }
}
