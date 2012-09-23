import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations
import org.jbox2d.collision._

package server {
  import shared._
  class Gib(server: Server, tank: Tank) extends Projectile(server, tank) {
    override val projectileType = ProjectileTypes.GIB

    override def shapes = {
      val polyDef = new PolygonDef
      polyDef.setAsBox(radius, radius*2)
      polyDef.restitution = 0f
      polyDef.density = 1f
      List(polyDef)
    }
  }
}
