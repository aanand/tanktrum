import server._

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

package shared {
  object Missile extends Item {
    override def name = "Missile"
    override def cost = 100
    override def units = 5
    override val projectileType = ProjectileTypes.MISSILE
  }
}

package server {
  import shared._
  class Missile(server: Server, tank: Tank) extends MachineGun(server, tank) {
    override val projectileType = ProjectileTypes.MISSILE

    if (tank != null) {
      tank.missile = this
    }

    override def collide(obj: GameObject, contact: ContactPoint) {
      collidedWith = obj
    }

    override def explode(obj: GameObject) {
      super.explode(obj)
      if (tank != null) {
        if (tank.missile == this) {
          tank.missile = null
        }
      }
    }
  }
}
