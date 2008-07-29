import org.newdawn.slick.opengl
import opengl.SlickCallable
import opengl.renderer.{Renderer, SGL}

abstract class GL {
  def draw(gl : SGL)
  
  val gl = Renderer.get
  import gl._
  
  SlickCallable.enterSafeBlock
  draw(gl)
  SlickCallable.leaveSafeBlock
  
  def shape(id : Int)(block : => Unit) {
    glBegin(id)
    block
    glEnd
  }
}