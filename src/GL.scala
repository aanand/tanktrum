import org.newdawn.slick.opengl
import opengl.SlickCallable
import opengl.renderer.{Renderer, SGL}

import org.lwjgl.opengl.GL11

object GL extends GL

class GL {
  val gl = Renderer.get
  import gl._
  
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
    safe {
      glBegin(id)
      block
      glEnd
    }
  }
  
  def safe(block: => Unit) {
    SlickCallable.enterSafeBlock
    block
    SlickCallable.leaveSafeBlock
  }
  
  def polygon(block: => Unit) = shape(GL11.GL_POLYGON)(block)
  def quadStrip(block: => Unit) = shape(GL11.GL_QUAD_STRIP)(block)

  def color(c: org.newdawn.slick.Color): Unit = color(c.r, c.g, c.b, c.a)
  def color(r: Float, g: Float, b: Float, a: Float): Unit = glColor4f(r, g, b, a)
  def vertex(x: Float, y: Float) = glVertex2f(x, y)
}