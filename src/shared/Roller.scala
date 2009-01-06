
import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

package shared {
  object RollerItem extends Item { 
    override def name = "Roller"
    override def cost = 100
    override def units = 5
    override val projectileType = ProjectileTypes.ROLLER
  }

  trait Roller extends Projectile {
    override val projectileType = ProjectileTypes.ROLLER
    //override val color = new slick.Color(0f, 0.3f, 0f)
    override lazy val radius = 0.8f
    override val damage = 10
    override val explosionRadius = 5f
    override val explosionDamageFactor = 1.2f

    override def imagePath = Config("projectile.roller.imagePath")
    override def imageWidth = Config("projectile.roller.imageWidth").toInt
    override def round = Config("projectile.roller.round").toBoolean
  }
}

package server {
  class Roller(server: Server, tank: Tank) extends Projectile(server, tank) with shared.Roller {
    //body.removeExcludedBody(session.ground.body)
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

package client {
  class Roller extends Projectile with shared.Roller
}
