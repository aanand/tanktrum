
import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

package shared {
  object Roller extends Item { 
    override def name = "Roller"
    override def cost = 100
    override def units = 5
    override val projectileType = ProjectileTypes.ROLLER
  }
}

package server {
  import shared._
  class Roller(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.ROLLER
    
    override def shapes = {
      val sDef = new CircleDef
      sDef.radius = radius
      sDef.restitution = 0.8f
      sDef.density = 1f
      List(sDef)
    }

    override def collide(obj: GameObject, contact: ContactPoint) {
      if (obj.isInstanceOf[Tank] || //Or it's hit a tank.
          (obj.isInstanceOf[Projectile] && !obj.isInstanceOf[Roller])) { //Or it's hit a projectile.)
          collidedWith = obj
      }
    }
  }
}
