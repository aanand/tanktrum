import org.newdawn.slick._
import java.io._

class Game(title: String) extends BasicGame(title) {
  var client: Client = _
  var server: Server = _
  var error: String = _
  
  var container: GameContainer = _

  var menu : Menu = _
  
  val userNameFile = new File("username")

  val INTRO_SOUND = "explosion.ogg"
  SoundPlayer.start
  

  def init(container: GameContainer) {
    this.container = container

    val serverPort = MenuEditable("10000", 5);
    val serverHostname = MenuEditable("localhost", 255)

    var storedUserName = "Player"
    if (userNameFile.exists) {
      val userNameBytes = new Array[Byte](Player.MAX_NAME_LENGTH)
      new FileInputStream(userNameFile).read(userNameBytes)
      storedUserName = new String(userNameBytes)
    }
    else {
      userNameFile.createNewFile
    }

    val userName = MenuEditable(storedUserName, Player.MAX_NAME_LENGTH)

    this.menu = new Menu(List(
      ("name", userName),
      ("start server", Submenu(List(
        ("port", serverPort),
        ("ok", MenuCommand(Unit => startServer(serverPort.value.toInt, userName.value)))))),
      ("connect", Submenu(List(
        ("hostname", serverHostname),
        ("port", serverPort),
        ("join", MenuCommand(Unit => startClient(serverHostname.value, serverPort.value.toInt, userName.value)))))),
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
  }

  def render(container: GameContainer, graphics: Graphics) {
    if (client != null && client.isActive) {
      client.render(graphics)
    }
    
    if (menu != null) {
      menu.render(container, graphics)
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
    new FileOutputStream(userNameFile).write(userName.getBytes)

    SoundPlayer ! PlaySound(INTRO_SOUND)
    if (client != null) {
      client.leave()
      client = null
    }

    client = new Client(address, port, userName, container)
    println("Starting client.")
    client.enter()
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
