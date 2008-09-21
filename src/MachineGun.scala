import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object MachineGunItem extends Item {
  override def name = "Machine Gun"
  override def cost = 100
  override def units = 50
  override val projectileType = ProjectileTypes.MACHINE_GUN
}

class MachineGun(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val radius = 3f
  override val mass = 0.2f
  override val explosionRadius = 4f
  override val damage = 3
  override val reloadTime = 0.4f
  override val projectileType = ProjectileTypes.MACHINE_GUN

  override def shape = new phys2d.raw.shapes.Box(radius, radius*2)
  
  override def update(delta : Int) {
    super.update(delta)
    if (session.isInstanceOf[Server]) {
      body.setRotation((Math.atan2(body.getVelocity.getX, -body.getVelocity.getY)).toFloat)
    }
  }

  override def renderBody(g: slick.Graphics) {
    g.setColor(color)
    g.translate(x, y)
    g.rotate(0, 0, body.getRotation.toDegrees)
    g.fillRect(-radius/2, -radius, radius, radius*2)
    g.fillOval(-radius/2, -3*radius/2, radius, radius)
    g.resetTransform
  }
}
