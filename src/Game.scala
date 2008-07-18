import org.newdawn.slick._

class Game(title : String) extends BasicGame(title) {
  var state: Session = _
  var error: String = _
  
  var container: GameContainer = _

  var menu : Menu = _
  
  def init(container: GameContainer) {
    this.container = container

    val serverPort = MenuEditable("10000");

    this.menu = new Menu(List(
      ("start server", Submenu(List(
        ("port", serverPort),
        ("ok", MenuCommand(Unit => startServer(serverPort.value.toInt)))))),
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
    
    state = new Server(port)
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
