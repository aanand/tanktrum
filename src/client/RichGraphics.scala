package client

import org.newdawn.slick._

class RichGraphics(g: Graphics) {
  def drawString(string: String, x: Int, y: Int, shadow: Boolean) {
    if (shadow) {
      val color = g.getColor
      g.setColor(new Color(0f, 0f, 0f))
      g.drawString(string, x+2, y+2)
      g.setColor(color)
    }

    g.drawString(string, x, y)
  }
}

object RichGraphics {
  implicit def graphics2RichGraphics(g: Graphics) = new RichGraphics(g)
}

