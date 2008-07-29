import org.newdawn.slick.opengl
import opengl.SlickCallable
import opengl.renderer.{Renderer, SGL}

import org.lwjgl.opengl.GL11

class GL {
  val gl = Renderer.get
  import gl._
  
  def shape(id: Int)(block: => Unit) {
    perform {
      glBegin(id)
      block
      glEnd
    }
  }
  
  def perform(block: => Unit) {
    SlickCallable.enterSafeBlock
    block
    SlickCallable.leaveSafeBlock
  }
  
  def polygon(block: => Unit) = shape(GL11.GL_POLYGON)(block)
  def quadStrip(block: => Unit) = shape(GL11.GL_QUAD_STRIP)(block)

  def color(r: Float, g: Float, b: Float, a: Float) = glColor4f(r, g, b, a)
  def vertex(x: Float, y: Float) = glVertex2f(x, y)
}