import org.newdawn.slick
import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._
import java.util.Date

import scala.collection.mutable.HashMap

import sbinary.Operations
import sbinary.Instances._

class Client (hostname: String, port: Int, name: String, container: GameContainer) extends Session(container) {
  val PING_PERIOD = 1000
  val SERVER_TIMEOUT = 10000
  val MAX_MESSAGES = 5

  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  var lastPing = new Date()
  var lastPong = new Date()

  val skyTopColor    = new Color(0f, 0f, 0.25f)
  val skyBottomColor = new Color(0.3f, 0.125f, 0.125f)

  val players = new HashMap[Short, Player]
  var me: Player = null

  var serverFull = false

  val readyRoom = new ReadyRoom(this)
  var inReadyRoom = true
  var chatInput = false
  val chatInputField = new ChatMenuEditable("", 512)
  val chatInputMenu = new ChatMenu(List(("Chat: ", chatInputField)))
  var chatMessages = List[String]()

  override def enter() = {
    super.enter()
    channel = DatagramChannel.open()
    channel.connect(new InetSocketAddress(hostname, port))
    channel.configureBlocking(false)

    sendHello
  }

  override def update(delta: Int) {
    super.update(delta)
    ping
    checkTimeout

    data.clear
    if (channel.receive(data) != null) {
      data.limit(data.position)
      data.rewind
      val command = data.get.toChar
      processCommand(command)
    }
  }

  override def tanks = {
    players.values.map(player => player.tank)
  }
  
  def render(g: Graphics) {
    if (serverFull) {
      g.setColor(new Color(1f, 0f, 0f))
      g.drawString("Error: Server full.", 300, 300)
      return
    }
    if (inReadyRoom) {
      readyRoom.render(g)
      return
    }
    if (ground.initialised) {
      renderSky(g)
      ground.render(g)
    }
    for (p <- projectiles) {
      p.render(g)
    }
    for (e <- explosions) {
      e.render(g)
    }
    for (f <- frags) {
      f.render(g)
    }
    for (p <- players.values) {
      p.render(g)
    }
    if (chatInput) {
      g.resetTransform
      g.translate(0, 560)
      chatInputMenu.render(g)
    }
    g.resetTransform
    g.translate(20, 560 - 15*chatMessages.length)
    g.setColor(new Color(0, 0, 1))
    for (message <- chatMessages) {
      g.translate(0, 15)
      g.drawString(message, 0, 0)
    }
  }
  
  def renderSky(g : Graphics) {
    new GL {
      polygon {
        color(skyTopColor.r, skyTopColor.g, skyTopColor.b, 1f)
        vertex(0, 0)
        vertex(container.getWidth, 0)
      
        color(skyBottomColor.r, skyBottomColor.g, skyBottomColor.b, 1f)
        vertex(container.getWidth, container.getHeight)
        vertex(0, container.getHeight)
      }
    }
  }
 
  def processCommand(command: Char) {
    command match {
      case Commands.SERVER_FULL => {serverFull = true}
      case Commands.GROUND => {loadGround}
      case Commands.PING   => {resetTimeout}
      case Commands.TANKS => {processUpdate}
      case Commands.PROJECTILE => {loadProjectile}
      case Commands.PROJECTILES => {loadProjectiles}
      case Commands.EXPLOSION => {loadExplosion}
      case Commands.PLAYERS => {loadPlayers}
      case Commands.READY_ROOM => {inReadyRoom = true}
      case Commands.CHAT_MESSAGE => {addChatMessage}
    }
  }
  
  def keyPressed(key : Int, char : Char) {
    if (inReadyRoom) {
      readyRoom.menu.keyPressed(key, char)
      return
    }
    if (chatInput) {
      key match {
        case Input.KEY_ENTER => { sendChat }
        case _ => { chatInputMenu.keyPressed(key, char) }
      }
      return
    }
    char match {
      case 'a' => { sendCommand(Commands.MOVE_LEFT) }
      case 'd' => { sendCommand(Commands.MOVE_RIGHT) }
      case _ => {
        key match {
          case Input.KEY_LEFT  => { sendCommand(Commands.AIM_ANTICLOCKWISE) }
          case Input.KEY_RIGHT => { sendCommand(Commands.AIM_CLOCKWISE) }
          case Input.KEY_UP    => { sendCommand(Commands.POWER_UP) }
          case Input.KEY_DOWN  => { sendCommand(Commands.POWER_DOWN) }
          case Input.KEY_SPACE => { sendCommand(Commands.FIRE) }
          case Input.KEY_TAB   => { sendCommand(Commands.CYCLE_WEAPON) }
          case Input.KEY_ENTER => { startChat }
          case _ => {}
        }
      }
    }
  }
  
  def keyReleased(key : Int, char : Char) {
    char match {
      case 'a' => { sendCommand(Commands.STOP_MOVE_LEFT) }
      case 'd' => { sendCommand(Commands.STOP_MOVE_RIGHT) }
      case _ => {
        key match {
          case Input.KEY_LEFT  => { sendCommand(Commands.STOP_AIM_ANTICLOCKWISE) }
          case Input.KEY_RIGHT => { sendCommand(Commands.STOP_AIM_CLOCKWISE) }
          case Input.KEY_UP    => { sendCommand(Commands.STOP_POWER_UP) }
          case Input.KEY_DOWN  => { sendCommand(Commands.STOP_POWER_DOWN) }
          case _ => {}
        }
      }
    }
  }

  def startChat = {
    chatInput = true
    chatInputField.value = ""
    chatInputField.perform(chatInputMenu)
  }

  def sendChat = {
    chatInput = false
    send(byteToArray(Commands.CHAT_MESSAGE) ++ Operations.toByteArray(chatInputField.value))
  }

  def addChatMessage = {
    val messageArray = new Array[byte](data.remaining)
    data.get(messageArray)
    val message = Operations.fromByteArray[String](messageArray)
    println("Got chat message: " + message)
    chatMessages += message
    if (chatMessages.length > MAX_MESSAGES) {
      chatMessages = chatMessages.tail
    }
  }
  
  def ping = {
    if (new Date().getTime - lastPing.getTime > PING_PERIOD) {
      sendPing
      lastPing = new Date()
    }
  }

  def checkTimeout = {
    if (new Date().getTime - lastPong.getTime > SERVER_TIMEOUT) {
      println("Connection timed out.")
      super.leave()
    }
  }

  def resetTimeout = {
    lastPong = new Date
  }

  def loadGround = {
    val groundArray = new Array[byte](data.remaining)
    if (ground != null && null != ground.body) {
      removeBody(ground.body)
    }
    data.get(groundArray)
    ground.loadFrom(groundArray)
  }

  def loadProjectile = {
    val projArray = new Array[byte](data.remaining)
    data.get(projArray)
    addProjectile(ProjectileLoader.loadProjectile(null, projArray, this))
  }

  def loadProjectiles = {
    val projArray = new Array[byte](data.remaining)
    data.get(projArray)

    val projDataList = Operations.fromByteArray[List[Array[byte]]](projArray)

    var i = 0

    projectiles = projDataList.map(projectileData => {
      var oldProjectile: Projectile = null
      if (projectiles.isDefinedAt(i)) {
        oldProjectile = projectiles(i)
      }
      i += 1
      ProjectileLoader.loadProjectile(oldProjectile, projectileData, this)
    })
  }

  def loadExplosion = {
    val explosionArray = new Array[byte](data.remaining)
    data.get(explosionArray)
    val e = new Explosion(0, 0, 0, this, null)
    e.loadFrom(explosionArray)
    explosions += e
  }

  def loadPlayers = {
    val playersArray = new Array[Byte](data.remaining)
    data.get(playersArray)
    val playerDataList = Operations.fromByteArray[List[Array[byte]]](playersArray)

    for (player <- players.values) { player.updated = false }

    for (playerData <- playerDataList) {
      val p = new Player(null, "Unknown.", 0)
      p.loadFrom(playerData)
      if (players.isDefinedAt(p.id) && players(p.id).name == p.name) {
        players(p.id).loadFrom(playerData)
      }
      else {
        players.put(p.id, p)
      }
      players(p.id).updated = true
      //This is poetry:
      if (p.me) {
        me = p
      }
    }

    //prune players who no longer exist.
    for (player <- players.values) {
      if (player.updated == false && !player.me) {
        players -= player.id
      }
    }
  }
  
  def processUpdate = {
    inReadyRoom = false
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    
    val tankDataList = Operations.fromByteArray[List[(Byte, Array[Byte])]](byteArray)
  
    for (tankDataMap <- tankDataList) {
      val (id, tankData) = tankDataMap
      if (players.isDefinedAt(id)) {
        if (null != players(id).tank) {
          players(id).tank.loadFrom(tankData)
        }
        else {
          val t = new Tank(this, 0)
          t.create(0)
          t.loadFrom(tankData)
          players(t.id).tank = t
        }
      }
    }
  }

  def sendCommand(command: Byte) {
    send(byteToArray(command))
  }

  def sendHello = {
    send(byteToArray(Commands.HELLO) ++ name.getBytes)
  }

  def sendPing = {
    send(byteToArray(Commands.PING))
  }

  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }
}
