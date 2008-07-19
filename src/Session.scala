import org.newdawn.slick._
import net.phys2d.math._
import net.phys2d.raw._

abstract class Session {
  val world = new World(new Vector2f(0.0f, 100.0f), 10)
  var ground : Ground = _
  var active = false
  
  def enter(container: GameContainer) {
    ground = new Ground(this, container.getWidth(), container.getHeight())
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def render(container: GameContainer, g: Graphics) {
    if (ground.initialised) {
      ground.render(g)
    }
  }
  
  def update(container: GameContainer, delta: Int) {
  }
  
  def keyPressed(key : Int, char : Char) {
  }

  def charToByteArray(c: Char) = {
    val a = new Array[byte](1)
    a(0) = c.toByte
    a
  }

  def isActive = active
}
