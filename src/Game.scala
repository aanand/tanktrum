import org.newdawn.slick

class Game(title : String) extends slick.BasicGame(title) {
  var state : Option[Session] = None
  var error : Option[String] = None
  
  var container : Option[slick.GameContainer] = None
  
  def init(container : slick.GameContainer) {
    this.container = Some(container)
    this.state = Some(new Server())
    
    state.get.enter(container)
  }

  def update(container : slick.GameContainer, delta : Int) {
    if (!state.isEmpty) {
      state.get.update(container, delta)
    }
  }

  def render(container : slick.GameContainer, graphics : slick.Graphics) {
    if (!state.isEmpty) {
      state.get.render(container, graphics)
    }
  }
}