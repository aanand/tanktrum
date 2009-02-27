package client
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
    glBegin(id)
    block
    glEnd
  }
  
  def safe(block: => Unit) {
    SlickCallable.enterSafeBlock
    block
    SlickCallable.leaveSafeBlock
  }
  
  def line(x1: Float, y1: Float, x2: Float, y2: Float) = {
    safe {
      glBegin(SGL.GL_LINES)
      glVertex2f(x1, y1)
      glVertex2f(x2, y2)
      glEnd()
    }
  }

  def lines(points: Array[(Float, Float)]) = {
    safe {
      glBegin(SGL.GL_LINES)
      for (i <- 0 until points.size-1) {
        val (x1, y1) = points(i)
        val (x2, y2) = points(i+1)
        glVertex2f(x1, y1)
        glVertex2f(x2, y2)
      }
      glEnd()
    }
  }

  def polygon(block: => Unit) = shape(GL11.GL_POLYGON)(block)
  def quadStrip(block: => Unit) = shape(GL11.GL_QUAD_STRIP)(block)
  def triStrip(block: => Unit) = shape(GL11.GL_TRIANGLE_STRIP)(block)

  def color(c: org.newdawn.slick.Color): Unit = color(c.r, c.g, c.b, c.a)
  def color(r: Float, g: Float, b: Float, a: Float): Unit = glColor4f(r, g, b, a)
  def color(r: Float, g: Float, b: Float): Unit = glColor4f(r, g, b, 1f)
  def vertex(x: Float, y: Float) = glVertex2f(x, y)
  def texture(x: Float, y: Float) = glTexCoord2f(x, y)
}
