import org.newdawn.slick._

class Game(title: String) extends BasicGame(title) {
  var state: Session = _
  var error: String = _
  
  var container: GameContainer = _

  var menu : Menu = _
  
  def init(container: GameContainer) {
    this.container = container

    val serverPort = MenuEditable("10000");
    val serverHostname = MenuEditable("localhost")
    val userName = MenuEditable("Player")

    this.menu = new Menu(List(
      ("name", userName),
      ("start server", Submenu(List(
        ("port", serverPort),
        ("ok", MenuCommand(Unit => startServer(serverPort.value.toInt)))))),
      ("connect", Submenu(List(
        ("hostname", serverHostname),
        ("port", serverPort),
        ("join", MenuCommand(Unit => startClient(serverHostname.value, serverPort.value.toInt, userName.value)))))),
      ("quit", MenuCommand(Unit => container.exit()))))
  }

  def update(container: GameContainer, delta: Int) {
    if (state != null && state.isActive) {
      state.update(delta)
    }
  }

  def render(container: GameContainer, graphics: Graphics) {
    if (state != null && state.isActive) {
      state.render(graphics)
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
    
    state = new Server(port, container)
    state.enter()
  }

  def startClient(address: String, port: Int, username: String) = {
    if (state != null) {
      state.leave()
      state = null
    }

    state = new Client(address, port, username, container)
    state.enter()
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
