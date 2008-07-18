import org.newdawn.slick._
import net.phys2d.math._
import net.phys2d.raw._

abstract class Session {
  val world = new World(new Vector2f(0.0f, 100.0f), 10)
  
  var ground : Ground = _
  
  def enter(container: GameContainer) {
  }
  
  def leave() {
  }
  
  def render(container: GameContainer, g: Graphics) {
    if (ground != null) {
      ground.render(g)
    }
  }
  
  def update(container: GameContainer, delta: Int) {
  }
  
  def keyPressed(key : Int, char : Char) {
  }
}
