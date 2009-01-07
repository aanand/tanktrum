import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

package shared {
  object MachineGun extends Item {
    override def name = "Machine Gun"
    override def cost = 100
    override def units = 50
    override val projectileType = ProjectileTypes.MACHINE_GUN
  }
}

package server {
  import shared._
  class MachineGun(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.MACHINE_GUN
    
    override def shapes = {
      val polyDef = new PolygonDef
      polyDef.setAsBox(radius/2, radius)
      polyDef.restitution = 0f
      polyDef.density = 1f
      List(polyDef)
    }
  }
}
