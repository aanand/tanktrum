package client
import org.lwjgl.opengl.GL11

object GL extends GL

class GL {
  import GL11._
  import Math._
  
  val defaultSegments = 32

  def translate(x: Float, y: Float)(f: => Unit) {
    glPushMatrix
    glTranslatef(x, y, 0)
    f
    glPopMatrix
  }
  
  def rotate(x: Float, y: Float, angle: Float)(f: => Unit) {
    translate(x, y) {
      glPushMatrix
      glRotatef(angle, 0, 0, 1)
      f
      glPopMatrix
    }
  }
  
  def scale(x: Float, y: Float)(f: => Unit) {
    glPushMatrix
    glScalef(x, y, 0)
    f
    glPopMatrix
  }
  
  def shape(id: Int)(block: => Unit) {
    glBegin(id)
    block
    glEnd
  }
  
  def line(x1: Float, y1: Float, x2: Float, y2: Float) = {
    glBegin(GL_LINES)
    glVertex2f(x1, y1)
    glVertex2f(x2, y2)
    glEnd()
  }

  def lines(points: Array[(Float, Float)]) = {
    glBegin(GL_LINES)
    for (i <- 0 until points.size-1) {
      val (x1, y1) = points(i)
      val (x2, y2) = points(i+1)
      glVertex2f(x1, y1)
      glVertex2f(x2, y2)
    }
    glEnd()
  }

  def texture(id: Int)(block: => Unit) = {
    glEnable(GL_TEXTURE_2D)
    glBindTexture(GL_TEXTURE_2D, id)
    block
    glDisable(GL_TEXTURE_2D)
  }

  def polygon(block: => Unit) = shape(GL_POLYGON)(block)
  def quadStrip(block: => Unit) = shape(GL_QUAD_STRIP)(block)
  def triStrip(block: => Unit) = shape(GL_TRIANGLE_STRIP)(block)
  def triFan(block: => Unit) = shape(GL_TRIANGLE_FAN)(block)

  def color(c: org.newdawn.slick.Color): Unit = color(c.r, c.g, c.b, c.a)
  def color(r: Float, g: Float, b: Float, a: Float): Unit = glColor4f(r, g, b, a)
  def color(r: Float, g: Float, b: Float): Unit = glColor4f(r, g, b, 1f)
  def vertex(x: Float, y: Float) = glVertex2f(x, y)
  def textureVertex(x: Float, y: Float) = glTexCoord2f(x, y)

  def oval (x1: Float, y1: Float, width: Float, height: Float) {
    oval(x1, y1, width, height, defaultSegments);
  }

  def oval(x1: Float, y1: Float, width: Float, height: Float, segments: Int) {
    arc(x1, y1, width, height, segments, 0, 2*Pi.toFloat)
  }

  def arc(x1: Float, y1: Float, width: Float, height: Float, start: Float, end: Float) {
    arc(x1, y1, width, height, defaultSegments, start, end)
  }

  def arc(x1: Float, y1: Float, width: Float, height: Float, segments: Int, start: Float, end: Float) = {
    val cx = x1 + (width / 2.0f)
    val cy = y1 + (height / 2.0f)

    triFan {
      val step = 2*Pi.toFloat / segments
      glVertex2f(cx, cy);
      
      var a = start
      while (a < end+step) {
        var ang = a

        if (ang > end) {
          ang = end
        }

        val x = cx + cos(ang).toFloat * width / 2f
        val y = cy + sin(ang).toFloat * height / 2f

        glVertex2f(x, y)
        a += step
      }
    }
  }
}
