import org.newdawn.slick._

class Game(title : String) extends BasicGame(title) {
  var state: Session = _
  var error: String = _
  
  var container: GameContainer = _
  
  def init(container: GameContainer) {
    this.container = container
    this.state = new Server(10000)
    
    state.enter(container)
  }

  def update(container: GameContainer, delta: Int) {
    if (state != null) {
      state.update(container, delta)
    }
  }

  def render(container: GameContainer, graphics: Graphics) {
    if (state != null) {
      state.render(container, graphics)
    }
  }
}
