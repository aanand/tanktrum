import org.newdawn.slick._
import java.io._
import java.util.prefs._

class Game(title: String) extends BasicGame(title) {
  val TICK = 10 //ms
  var client: Client = _
  var server: Server = _
  var error: String = _
  
  var container: GameContainer = _

  var menu : Menu = _
  
  val INTRO_SOUND = "explosion.ogg"
  SoundPlayer.start
  
  val prefs = Preferences.userRoot.node("boomtrapezoid")

  var titleImage: Image = _

  def init(container: GameContainer) {
    this.container = container

    val storedUserName = prefs.get("username", "Player")
    val storedPort = prefs.get("port", "10000")
    val storedHostname = prefs.get("hostname", "boomtrapezoid.com")

    val serverPort = MenuEditable(storedPort, 5);
    val serverHostname = MenuEditable(storedHostname, 255)
    val userName = MenuEditable(storedUserName, Player.MAX_NAME_LENGTH)

    titleImage = new Image("media/images/title.png")

    this.menu = new Menu(List(
      ("name", userName),
      ("start server", Submenu(List(
        ("port", serverPort),
        ("ok", MenuCommand(Unit => startServer(serverPort.value.toInt, userName.value)))))),
      ("connect", Submenu(List(
        ("hostname", serverHostname),
        ("port", serverPort),
        ("join", MenuCommand(Unit => startClient(serverHostname.value, serverPort.value.toInt, userName.value)))))),
      ("practice", MenuCommand(Unit => startPractice(userName.value))),
      ("quit", MenuCommand(Unit => container.exit()))))
  }

  def update(container: GameContainer, delta: Int) {
    //println("Updating: " + new java.util.Random().nextInt)
    if (client != null && client.isActive) {
      client.update(delta)
    }
    if (server != null && server.isActive) {
      server.update(delta)
    }
    if (TICK-delta > 0) {
      Thread.sleep(TICK-delta)
    }
  }

  def render(container: GameContainer, graphics: Graphics) {
    if (menu != null && menu.showing) {
      graphics.drawImage(titleImage, 0, 0)
      menu.render(graphics)
      return
    }
    if (client != null && client.isActive) {
      client.render(graphics)
    }
  }
  
  def startServer(port : Int, userName : String) {
    if (server != null) {
      server.leave
      server = null
    }
   
    server = new Server(port)
    println("Starting server.")
    server.enter

    startClient("localhost", port, userName)
  }

  def startClient(address: String, port: Int, userName: String) = {
    prefs.put("username", userName)
    prefs.put("hostname", address)
    prefs.put("port", port.toString)

    SoundPlayer ! PlaySound(INTRO_SOUND)
    if (client != null) {
      client.leave()
      client = null
    }

    client = new Client(address, port, userName, container)
    println("Starting client.")
    client.enter()
  }
  
  def startPractice(userName: String) {
    if (server != null) {
      server.leave
      server = null
    }

    val port = 10000

    server = new PracticeServer(port)
    println("Starting practice server.")
    server.enter

    startClient("localhost", port, userName)
  }
  
  override def keyPressed(key : Int, char : Char) {
    if (menu.showing) {
      menu.keyPressed(key, char)
    } else if (key == Input.KEY_ESCAPE) {
      menu.show()
    } else if (client != null) {
      client.keyPressed(key, char)
    }
  }
  
  override def keyReleased(key : Int, char : Char) {
    if (client != null) {
      client.keyReleased(key, char)
    }
  }
}
