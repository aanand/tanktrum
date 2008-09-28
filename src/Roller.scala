import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object RollerItem extends Item { 
  override def name = "Roller"
  override def cost = 100
  override def units = 5
  override val projectileType = ProjectileTypes.ROLLER
}

class Roller(session : Session, tank : Tank) extends Projectile(session, tank) {
  override val projectileType = ProjectileTypes.ROLLER
  override val color = new slick.Color(0.4f, 0.6f, 0f)
  override val damage = 10
  override val explosionRadius = 25f

  //body.removeExcludedBody(session.ground.body)
  override def shapes = {
    val sDef = new CircleDef
    sDef.radius = radius
    sDef.restitution = 1f
    sDef.density = 1f
    List(sDef)
  }

  override def collide(obj: GameObject, contact: ContactPoint) {
    if (y - radius > session.ground.heightAt(x) || //It's below the ground.
        obj.isInstanceOf[Tank] || //Or it's hit a tank.
        (obj.isInstanceOf[Projectile] && !obj.isInstanceOf[Roller])) { //Or it's hit a projectile.)
        collidedWith = obj
    }
  }
}
