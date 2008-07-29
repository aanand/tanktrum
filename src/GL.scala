import org.newdawn.slick.opengl
import opengl.SlickCallable
import opengl.renderer.{Renderer, SGL}

class GL {
  val gl = Renderer.get
  import gl._
  
  def shape(id : Int)(block : => Unit) {
    perform {
      glBegin(id)
      block
      glEnd
    }
  }
  
  def perform(block : => Unit) {
    SlickCallable.enterSafeBlock
    block
    SlickCallable.leaveSafeBlock
  }
  
  def color(r : Float, g : Float, b : Float, a : Float) = glColor4f(r, g, b, a)
  def vertex(x : Float, y : Float) = glVertex2f(x, y)
}