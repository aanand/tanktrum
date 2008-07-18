import org.newdawn.slick._

class Game(title : String) extends BasicGame(title) {
  var state: Session = _
  var error: String = _
  
  var container: GameContainer = _

  var menu : Menu = _
  
  def init(container: GameContainer) {
    this.container = container

    this.menu = new Menu(List(
      ("start server", Submenu(List(
        ("port", MenuEditable("10000")),
        ("ok", MenuCommand(Unit => startServer(10000)))))),
      ("quit", MenuCommand(Unit => container.exit()))))
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
    
    if (menu != null) {
      menu.render(container, graphics)
    }
  }
  
  def startServer(port : Int) {
    if (state != null) {
      state.leave()
      state = null
    }
    
    state = new Server(10000)
    state.enter(container)
  }
  
  override def keyPressed(key : Int, char : Char) {
    if (menu.showing) {
      menu.keyPressed(key, char)
    } else if (key == Input.KEY_ESCAPE) {
      menu.show()
    } else if (state != null) {
      state.keyPressed(key, char)
    }
  }
}
