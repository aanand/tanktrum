import org.newdawn.slick
import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._

import scala.collection.mutable.HashMap

import sbinary.Operations
import sbinary.Instances._

import ClientTank._

class Client (hostname: String, port: Int, name: String, container: GameContainer) extends Session(container) {
  val PING_PERIOD = Config("client.pingPeriod").toInt
  val TANK_UPDATE_INTERVAL = Config("client.tankUpdateInterval").toInt
  val SERVER_TIMEOUT = Config("client.serverTimeout").toInt

  val CHAT_KEY              = Input.KEY_T
  val MOVE_LEFT_KEY         = Input.KEY_A
  val MOVE_RIGHT_KEY        = Input.KEY_D
  val JUMP_KEY              = Input.KEY_W
  val AIM_ANTICLOCKWISE_KEY = Input.KEY_LEFT
  val AIM_CLOCKWISE_KEY     = Input.KEY_RIGHT
  val POWER_UP_KEY          = Input.KEY_UP
  val POWER_DOWN_KEY        = Input.KEY_DOWN
  val FIRE_KEY              = Input.KEY_SPACE
  val CYCLE_WEAPON_KEY      = Input.KEY_Q

  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  
  var lastPing = System.currentTimeMillis
  var lastMessageReceived = System.currentTimeMillis
  var timeToTankUpdate = 0
  var lastTankUpdate = new Array[Byte](6)
  var latency: Long = 0

  val players = new HashMap[Short, Player]
  var me: Player = null

  var errorState = false
  var errorMessage = ""

  val readyRoom = new ReadyRoom(this)
  var inReadyRoom = true
  
  val chat = new Chat(this)
  
  val tankSequence = new Sequence
  val projectileSequence = new Sequence
  val groundSequence = new Sequence

  val particleSystem = new slick.particles.ParticleSystem(makeParticleImage)

  val imageFilename = (new java.util.Random().nextInt(7) + 1).toString + ".jpg"

  val skyImage = new Image("media/sky/" + imageFilename)
  val groundImage = new Image("media/ground/" + imageFilename)

  // force groundImage.init() (which is private) to be called. I know, wtf.
  groundImage.toString

  override def enter {
    super.enter()
    channel = DatagramChannel.open()
    try {
      channel.connect(new InetSocketAddress(hostname, port))
      channel.configureBlocking(false)
    }
    catch {
      case e: UnresolvedAddressException => {
        error("Could not resolve server name, please try again.")
        return
      }
      case e: Exception => { 
        e.printStackTrace()
        error(e.toString) 
        return
      }
    }

    sendHello
  }

  override def leave {
    super.leave()
    if (!channel.isConnected) return
    sendCommand(Commands.GOODBYE)
  }

  override def update(delta: Int) {
    if (!channel.isConnected) return
    try {
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

      timeToTankUpdate -= delta
      if (timeToTankUpdate < 0) {
        if (null != me && null != me.tank) {
          timeToTankUpdate = TANK_UPDATE_INTERVAL
          sendTankUpdate
        }
      }
      
      for (p <- projectiles.values) {
        if (p.dead && p.trailDead) {
          removeProjectile(p)
        }
      }
      
      particleSystem.update(delta)
    }
    catch {
      case e:Exception => { 
        e.printStackTrace()
        error(e.toString) 
      }
    }
  }

  override def tanks = players.values.map(player => player.tank)
  
  def render(g: Graphics) {
    if (errorState) {
      g.setColor(new Color(1f, 0f, 0f))
      g.drawString(errorMessage, 300, 300)
      return
    }

    if (inReadyRoom) {
      readyRoom.render(g)
    }
    else { 
      if (ground.initialised) {
        renderSky(g)
      }

      g.scale(Main.GAME_WINDOW_RATIO, Main.GAME_WINDOW_RATIO)

      if (ground.initialised) {
        particleSystem.render()
        ground.render(g, groundImage)
      }
      
      projectiles.values.foreach(_.render(g))
      explosions.foreach        (_.render(g))
      players.values.foreach    (_.render(g))
      players.values.foreach    ((player) => if (null != player.tank) {player.tank.render(g)})
    }

    g.resetTransform
    g.setColor(new Color(1f, 1f, 1f))
    g.drawString("Ping: " + latency, 735, 575)

    chat.render(g)
  }
  
  def renderSky(g : Graphics) {
    g.drawImage(skyImage, 0, 0)
  }
  
  def makeParticleImage = {
    val dot = new ImageBuffer(3, 3)

    for (x <- 0 until 3) {
      for (y <- 0 until 3) {
        dot.setRGBA(x, y, 255, 255, 255, 0)
      }
    }

    dot.setRGBA(1, 1, 255, 255, 255, 100)
    
    new Image(dot)
  }

  def error(message: String) {
    errorState = true
    errorMessage = message
    if (null == errorMessage) { errorMessage = "Unknown error." }
  }
 
  def processCommand(command: Char) {
    resetTimeout
    command match {
      case Commands.SERVER_FULL  => error("Server full.")
      case Commands.GROUND       => loadGround
      case Commands.PING         => latency = System.currentTimeMillis - lastPing
      case Commands.TANKS        => processUpdate
      case Commands.PROJECTILE   => loadProjectile
      case Commands.PROJECTILES  => loadProjectiles
      case Commands.EXPLOSION    => loadExplosion
      case Commands.PLAYERS      => loadPlayers
      case Commands.READY_ROOM   => {
        if (!inReadyRoom) {
          inReadyRoom = true
          endRound()
        }
      }
      case Commands.CHAT_MESSAGE => addChatMessage
      case _                     => println("Warning: Client got unknown command: " + command.toByte)
    }
  }
  
  def keyPressed(key : Int, char : Char) {
    try {
      if (chat.input) {
        chat.keyPressed(key, char)
        return
      }
      if (inReadyRoom) {
        if (key == CHAT_KEY) { chat.start }
        else { readyRoom.menu.keyPressed(key, char) }
        return
      }
      else {
        key match {
          case CHAT_KEY              => chat.start 
          case MOVE_LEFT_KEY         => sendCommand(Commands.MOVE_LEFT) 
          case MOVE_RIGHT_KEY        => sendCommand(Commands.MOVE_RIGHT) 
          case JUMP_KEY              => sendCommand(Commands.JUMP) 
          case AIM_ANTICLOCKWISE_KEY => me.tank.gun.angleChange = -1
          case AIM_CLOCKWISE_KEY     => me.tank.gun.angleChange = 1
          case POWER_UP_KEY          => me.tank.gun.powerChange = 1
          case POWER_DOWN_KEY        => me.tank.gun.powerChange = -1
          case FIRE_KEY              => sendCommand(Commands.START_FIRE) 
          case CYCLE_WEAPON_KEY      => sendCommand(Commands.CYCLE_WEAPON) 
          case _                     => 
        }
      }
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        error(e.toString)
      }
    }
  }
  
  def keyReleased(key : Int, char : Char) {
    try {
      if (!inReadyRoom) {
        key match {
          case MOVE_LEFT_KEY         => sendCommand(Commands.STOP_MOVE_LEFT) 
          case MOVE_RIGHT_KEY        => sendCommand(Commands.STOP_MOVE_RIGHT) 
          case JUMP_KEY              => sendCommand(Commands.STOP_JUMP) 
          case AIM_ANTICLOCKWISE_KEY => if (me.tank.gun.angleChange == -1) {me.tank.gun.angleChange = 0}
          case AIM_CLOCKWISE_KEY     => if (me.tank.gun.angleChange ==  1) {me.tank.gun.angleChange = 0}
          case POWER_UP_KEY          => if (me.tank.gun.powerChange ==  1) {me.tank.gun.powerChange = 0}
          case POWER_DOWN_KEY        => if (me.tank.gun.powerChange == -1) {me.tank.gun.powerChange = 0}
          case FIRE_KEY              => sendCommand(Commands.STOP_FIRE) 
          case _                     => 
        }
      }
    }
    catch {
      case e: Exception =>  {
        e.printStackTrace()
        error(e.toString)
      }
    }
  }
  
  def mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    if (inReadyRoom) {
      readyRoom.mouseMoved(oldx, oldy, newx, newy)
    }
  }
  
  def mouseClicked(button: Int, x: Int, y: Int, clickCount: Int) {
    if (inReadyRoom) {
      readyRoom.mouseClicked(button, x, y, clickCount)
    }
  }
  
  /*
   * Everything below here is basically networking stuff.
   */

  def addChatMessage = {
    val messageArray = new Array[byte](data.remaining)
    data.get(messageArray)
    val message = Operations.fromByteArray[String](messageArray)
    println("Got chat message: " + message)
    chat.add(message)
  }
  
  def ping = {
    if (System.currentTimeMillis - lastPing> PING_PERIOD) {
      sendPing
      lastPing = System.currentTimeMillis
    }
  }
  
  def checkTimeout = {
    if (System.currentTimeMillis - lastMessageReceived > SERVER_TIMEOUT) {
      println("Connection timed out.")
      leave()
    }
  }

  def resetTimeout = {
    lastMessageReceived = System.currentTimeMillis
  }

  def loadGround {
    val groundArray = new Array[byte](data.remaining)
    data.get(groundArray)
    val (seq, shortPoints) = Operations.fromByteArray[(Short, Array[Short])](groundArray)
    if (!groundSequence.inOrder(seq)) {
      return
    }

    if (null != ground) {
      ground.loadFrom(shortPoints)
    }
  }

  def loadProjectile = {
    val projData = new Array[byte](data.remaining)
    data.get(projData)
    loadProjectileFromDataArray(projData)
  }

  def loadProjectiles {
    val projArray = new Array[byte](data.remaining)
    data.get(projArray)

    val (seq, projDataArray) = Operations.fromByteArray[(Short, Array[Array[byte]])](projArray)

    if (!projectileSequence.inOrder(seq)) {
      return
    }

    var liveProjectileIds: List[Int] = Nil

    for (projData <- projDataArray) {
      val id = loadProjectileFromDataArray(projData)
      
      liveProjectileIds = id :: liveProjectileIds
    }
    
    for (id <- projectiles.keys) {
      if (!liveProjectileIds.contains(id) && !projectiles(id).dead) {
        projectiles(id).dead = true
      }
    }
  }
  
  def loadProjectileFromDataArray(projData: Array[byte]): Int = {
    val tuple = Projectile.deserialise(projData)
    val id = tuple._1
    
    if (projectiles.isDefinedAt(id)) {
      projectiles(id).updateFromTuple(tuple)
    } else {
      projectiles.put(id, Projectile.newFromTuple(this, tuple))
    }
    
    id
  }

  def loadExplosion = {
    val explosionArray = new Array[byte](data.remaining)
    data.get(explosionArray)
    val e = new Explosion(0, 0, 0, this, null, 0)
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
    }

    //prune players who no longer exist.
    for (player <- players.values) {
      if (player.updated == false && !player.me) {
        players -= player.id
      }
      if (player.updated == true && player.me) {
        me = player
      }
    }
  }
  
  def processUpdate {
    numTankUpdates += 1
    
    if (inReadyRoom) {
      inReadyRoom = false
      startTime = System.currentTimeMillis
      supposedRunTime = 0
      numTankUpdates = 0
    }
    
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    
    val (seq, tankDataList) = Operations.fromByteArray[(Short, List[(Byte, Array[Byte])])](byteArray)
    if (!tankSequence.inOrder(seq)) {
      return
    }
  
    for (tankDataMap <- tankDataList) {
      val (id, tankData) = tankDataMap
      if (players.isDefinedAt(id)) {
        if (null != players(id).tank) {
          players(id).tank.loadFrom(tankData)
        }
        else {
          val t = new ClientTank(this)
          t.create(0)
          t.loadFrom(tankData)
          players(t.id).tank = t
        }
      }
    }
  }

  def sendTankUpdate {
    val tankUpdate = me.tank.serialise
    if (!tankUpdate.toArray.deepEquals(lastTankUpdate.toArray)) {
      send(byteToArray(Commands.TANK_UPDATE) ++ tankUpdate)
      lastTankUpdate = tankUpdate
    }
  }

  def sendPurchase(item: byte) {
    send(byteToArray(Commands.BUY) ++ Operations.toByteArray(item))
  }

  def sendCommand(command: Byte) {
    send(byteToArray(command))
  }

  def sendChatMessage(message: String) {
    send(byteToArray(Commands.CHAT_MESSAGE) ++ Operations.toByteArray(message))
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
