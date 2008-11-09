import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object MissileItem extends Item {
  override def name = "Missile"
  override def cost = 100
  override def units = 5
  override val projectileType = ProjectileTypes.MISSILE
}

class Missile(session: Session, tank: Tank) extends MachineGun(session, tank) {
  override lazy val radius = 1f
  override val explosionRadius = 6f
  override val damage = 8
  override val reloadTime = 6f
  override val projectileType = ProjectileTypes.MISSILE

  override def imagePath = Config("projectile.missile.imagePath")
  override def imageWidth = Config("projectile.missile.imageWidth").toInt
  override def round = Config("projectile.missile.round").toBoolean

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
 
