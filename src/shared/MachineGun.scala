package shared
import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object MachineGunItem extends Item {
  override def name = "Machine Gun"
  override def cost = 100
  override def units = 50
  override val projectileType = ProjectileTypes.MACHINE_GUN
}

class MachineGun(session: Session, tank: server.Tank) extends Projectile(session, tank) {
  override lazy val radius = 0.4f
  override val explosionRadius = 0.8f
  override val damage = 3
  override val reloadTime = 0.4f
  override val projectileType = ProjectileTypes.MACHINE_GUN

  override def imagePath = Config("projectile.machineGun.imagePath")
  override def imageWidth = Config("projectile.machineGun.imageWidth").toInt
  override def round = Config("projectile.missile.round").toBoolean

  override def shapes = {
    val polyDef = new PolygonDef
    polyDef.setAsBox(radius/2, radius)
    polyDef.restitution = 0f
    polyDef.density = 1f
    List(polyDef)
  }
}
