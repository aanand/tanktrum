package shared {
  object CorbomiteItem extends Item {
    override def name = "Corbomite"
    override def cost = 50
    override def units = 8
  }

  trait Corbomite extends Projectile {
    override val radius = 0.4f
    override val explosionRadius = 2f
    override val projectileType = ProjectileTypes.CORBOMITE
  }
}

package server {
  class Corbomite(server: Server, tank: Tank) extends Projectile(server, tank) with shared.Corbomite
}

package client {
  class Corbomite extends Projectile with shared.Corbomite
}
