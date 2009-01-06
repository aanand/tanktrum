package client;
import org.newdawn.slick.geom._
import org.newdawn.slick._

class Gun(client: Client, tank: Tank) extends shared.Gun(client, tank) {
  val arrowShape = new Polygon(List[Float](-1, 0, -1, -10, -2, -10, 0, -12, 2, -10, 1, -10, 1, 0).toArray)
  
  val READY_COLOR   = new Color(0.0f, 1.0f, 0.0f, 0.5f)
  val LOADING_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.5f)

  def render(g: Graphics) {
    import GL._
    
    translate(OFFSET_X, OFFSET_Y) {
      rotate(0, 0, angle) {
        scale(1, power/POWER_SCALE) {
          g.setColor(if (ready) READY_COLOR else LOADING_COLOR)
          g.fill(arrowShape)
        }
      }
    }
  }
}
