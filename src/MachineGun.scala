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

class MachineGun(session: Session, tank: Tank) extends Projectile(session, tank) {
  override lazy val radius = 0.4f
  override val explosionRadius = 0.8f
  override val damage = 3
  override val reloadTime = 0.4f
  override val projectileType = ProjectileTypes.MACHINE_GUN

  override def shapes = {
    val polyDef = new PolygonDef
    polyDef.setAsBox(radius/2, radius)
    polyDef.restitution = 0f
    polyDef.density = 1f
    List(polyDef)
  }
  
  override def update(delta : Int) {
    super.update(delta)
    if (session.isInstanceOf[Server]) {
      body.setXForm(body.getPosition, (Math.atan2(body.getLinearVelocity.x, -body.getLinearVelocity.y)).toFloat)
    }
  }

  override def renderBody(g: slick.Graphics) {
    g.setColor(color)
    g.translate(x, y)
    g.rotate(0, 0, body.getAngle.toDegrees)
    g.fillRect(-radius/2, -radius, radius, radius*2)
    g.fillOval(-radius/2, -3*radius/2, radius, radius)
    g.resetTransform
    g.scale(Main.GAME_WINDOW_RATIO, Main.GAME_WINDOW_RATIO)
  }
}
