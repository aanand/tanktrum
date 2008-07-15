import org.newdawn.slick
import net.phys2d

abstract class Session {
  val world = new phys2d.raw.World(new phys2d.math.Vector2f(0.0f, 100.0f), 10)
  
  var ground : Option[Ground] = None
  
  def enter(container : slick.GameContainer) {
  }
  
  def leave() {
  }
  
  def render(container : slick.GameContainer, g : slick.Graphics) {
    if (!ground.isEmpty) {
      ground.get.render(g)
    }
  }
  
  def update(container : slick.GameContainer, delta : Int) {
  }
}