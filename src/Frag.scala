import org.newdawn.slick
import net.phys2d

class Frag (x: Float, y: Float, radius: Float, color: slick.Color) extends Collider {
  val shape = new phys2d.raw.shapes.Circle(radius)
  val body = new phys2d.raw.Body(shape, 1)
  body.setPosition(x, y)

  def render(g: slick.Graphics) {
    g.resetTransform
    g.translate(body.getPosition.getX, body.getPosition.getY)
    g.setColor(color)
    g.fillOval(-radius, -radius, radius*2, radius*2)
  }
}
