import org.newdawn.slick
import net.phys2d
import sbinary.Instances._
import sbinary.Operations

object MIRV {
  val name = "MIRV"
  val cost = 50
  val ammo = 1
}

class MIRV(session: Session, tank: Tank) extends Projectile(session, tank) {
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
