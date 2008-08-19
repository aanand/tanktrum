import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object MIRVItem extends Item {
  override def name = "MIRV"
  override def cost = 100
  override def units = 1
  override val projectileType = ProjectileTypes.MIRV
}

class MIRV(session: Session, tank: Tank) extends Projectile(session, tank) {
  override val projectileType = ProjectileTypes.MIRV

  override def update(delta: Int) {
    super.update(delta)
    if (session.isInstanceOf[Server]) {
      if (body.getVelocity.getY > 0) {
        for (val i <- -2 until 3) {
          val p = new Projectile(session, tank)
          p.body.setPosition(x, y)
          p.body.adjustVelocity(new phys2d.math.Vector2f(body.getVelocity.getX + i*5, body.getVelocity.getY))
          session.addProjectile(p)
        }
        session.removeProjectile(this)
      }
    }
  }
}
