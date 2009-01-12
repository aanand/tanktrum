package shared

import client._
import server._

import org.newdawn.slick._
import java.io._
import java.util.prefs._
import java.util.Date

class Game(title: String) extends BasicGame(title) {
  var client: Client = _
  var server: Server = _
  
  var container: GameContainer = _

  var menu : Menu = _
  val serverList  = new ServerList(this)
  
  SoundPlayer.start
  
  val prefs = Preferences.userRoot.node("boomtrapezoid")

  var titleImage: Image = _
  var font: Font = _

  def init(container: GameContainer) {
    this.container = container

    container.getInput.enableKeyRepeat(Config("game.keyRepeatWait").toInt, Config("game.keyRepeatInterval").toInt)

    val startFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,
                      new java.io.BufferedInputStream(Resource.get("media/fonts/" + Config("gui.fontFile"))))

    val baseFont  = startFont.deriveFont(java.awt.Font.PLAIN, Config("gui.fontSize").toInt)

    font = new TrueTypeFont(baseFont, Config("gui.fontSmooth").toBoolean)

    val storedUserName = prefs.get("username", "Player")
    val storedPort = prefs.get("port", Config("default.port"))
    val storedHostname = prefs.get("hostname", Config("default.hostname"))
    val storedServerPort = prefs.get("serverPort", Config("server.port"))
    val storedServerName = prefs.get("serverName", Config("server.name"))
    val storedServerPublic = prefs.get("serverPublic", Config("server.public")).toBoolean

    val userName = MenuEditable(storedUserName, Player.MAX_NAME_LENGTH)
    val serverPort = MenuEditable(storedServerPort, 5)
    val serverHostname = MenuEditable(storedHostname, 255)
    val serverName = MenuEditable(storedServerName, 127)
    val serverPublic = MenuToggle(storedServerPublic)

    titleImage = new Image("media/images/title.png")

    this.menu = new Menu(List(
      ("Name", userName),
      ("Find Server", MenuCommand(Unit => listServers(userName.value))),
      ("Start Server", Submenu(List(
        ("Name", serverName),
        ("Port", serverPort),
        ("Public", serverPublic),
        ("Ok", MenuCommand(Unit => startServer(serverPort.value.toInt, userName.value, serverName.value, serverPublic.value)))))),
      ("Connect", Submenu(List(
        ("Hostname", serverHostname),
        ("Port", serverPort),
        ("Join", MenuCommand(Unit => startClient(serverHostname.value, serverPort.value.toInt, userName.value)))))),
      ("Keys", Submenu(KeyCommands.toList)),
      ("Practice", MenuCommand(Unit => startPractice(userName.value))),
      ("Quit", MenuCommand(Unit => quit))))
  }

  def update(container: GameContainer, delta: Int) {
    //println("Updating: " + new java.util.Random().nextInt)
    if (client != null && client.active) {
      client.update(delta)
    } 
    
    if (serverList.showing) {
      serverList.update()
    }
  }

  def render(container: GameContainer, g: Graphics) {
    g.setFont(font)

    if (menu != null && menu.showing) {
      g.drawImage(titleImage, 0, 0)
      menu.render(g)
      return
    }
    else if (serverList.showing) {
      g.drawImage(titleImage, 0, 0, new Color(1f, 1f, 1f, 0.2f))
      serverList.render(g)
    }
    else if (client != null && client.active) {
      client.render(g)
    }
  }
  
  def startServer(port: Int, userName: String, serverName: String, public: Boolean) {
    prefs.put("serverPort", port.toString)
    prefs.put("serverName", serverName)
    prefs.put("serverPublic", public.toString)

    if (server != null) {
      server !? 'leave
      server = null
    }
   
    server = new Server(port, serverName, public)
    server.start
    println("Starting server.")
    server !? 'enter

    startClient("localhost", port, userName)
  }

  def startClient(address: String, port: Int, userName: String) = {
    prefs.put("username", userName)
    prefs.put("hostname", address)
    prefs.put("port", port.toString)

    if (client != null) {
      client.leave()
      client = null
    }

    client = new Client(address, port, userName, container)
    println("Starting client.")
    client.enter
  }
  
  def startPractice(userName: String) {
    if (server != null) {
      server !? 'leave
      server = null
    }

    val port = Config("default.port").toInt

    server = new PracticeServer(port)
    server.start
    println("Starting practice server.")
    server !? 'enter

    startClient("localhost", port, userName)
  }

  def listServers(userName: String) {
    serverList.show(userName)
  }
  
  override def keyPressed(key : Int, char : Char) {
    if (menu.showing) {
      menu.keyPressed(key, char)
    } else if (serverList.showing) {
      serverList.keyPressed(key, char)
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
  
  override def mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    if (menu.showing) {
      menu.mouseMoved(oldx, oldy, newx, newy)
    } else if (serverList.showing) {
      serverList.mouseMoved(oldx, oldy, newx, newy)
    } else if (client != null) {
      client.mouseMoved(oldx, oldy, newx, newy)
    }
  }
  
  override def mouseClicked(button: Int, x: Int, y: Int, clickCount: Int) {
    if (menu.showing) {
      menu.mouseClicked(button, x, y, clickCount)
    } else if (serverList.showing) {
      serverList.mouseClicked(button, x, y, clickCount)
    } else if (client != null) {
      client.mouseClicked(button, x, y, clickCount)
    }
  }

  def quit {
    if (client != null) {
      client.leave
    }
    if (server != null) {
      server !? 'leave
    }
    if (container != null) {
      container.exit
    }
  }

  override def closeRequested = {
    if (super.closeRequested) {
      if (null != client) {
        client.leave
      }
      
      if (null != server) {
        server !? 'leave
      }
      
      true
    } else {
      false
    }
  }
}
