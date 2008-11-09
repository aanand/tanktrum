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
  //override val color = new slick.Color(0f, 0.3f, 0f)
  override lazy val radius = 0.8f
  override val damage = 10
  override val explosionRadius = 5f
  override val explosionDamageFactor = 1.2f

  override def imagePath = Config("projectile.roller.imagePath")
  override def imageWidth = Config("projectile.roller.imageWidth").toInt
  override def round = Config("projectile.roller.round").toBoolean

  //body.removeExcludedBody(session.ground.body)
  override def shapes = {
    val sDef = new CircleDef
    sDef.radius = radius
    sDef.restitution = 0.8f
    sDef.density = 1f
    List(sDef)
  }

  override def renderBody(g: slick.Graphics) {
    super.renderBody(g)
    g.setColor(new slick.Color(0f, 1f, 0f))
    g.fillOval(x - radius/4, y-radius/4, radius/2, radius/2)
  }

  override def collide(obj: GameObject, contact: ContactPoint) {
    if (obj.isInstanceOf[Tank] || //Or it's hit a tank.
        (obj.isInstanceOf[Projectile] && !obj.isInstanceOf[Roller])) { //Or it's hit a projectile.)
        collidedWith = obj
    }
  }
}
