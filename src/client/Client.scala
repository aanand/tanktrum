package client
import org.newdawn.slick
import org.newdawn.slick._
import shared._

import java.nio.channels._
import java.nio._
import java.net._

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import sbinary.Operations
import sbinary.Instances._

class Client (hostname: String, port: Int, name: String, container: GameContainer) extends Session {
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
  
  var ground: Ground = _

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

  var imageSetIndex = 0
  var skyImage: Image = _
  var groundImage: Image = _
  
  var projectiles = new HashMap[Int, Projectile]
  var explosions = new HashSet[Explosion]

  var previousProjectileUpdate = 0L
  var currentProjectileUpdate = 0L

  def enter {
    active = true
    ground = new Ground(Main.GAME_WIDTH.toInt, Main.GAME_HEIGHT.toInt)
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

  def leave() {
    active = false
    if (!channel.isConnected) return
    sendCommand(Commands.GOODBYE)
  }
  
  def update(delta: Int) {
    if (!channel.isConnected) return
    try {
      ping
      checkTimeout
      
      for (p <- projectiles.values) {
        p.update(delta)
      }
      for (tank <- tanks) {
        if (null != tank) { tank.update(delta) }
      }
      for (e <- explosions) {
        e.update(delta)
      }

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
  
  def tanks = players.values.map(player => player.tank)

  def removeExplosion(e: Explosion) {
    explosions -= e
  }
  
  def removeProjectile(p : Projectile) {
    projectiles -= p.id
  }
  
  def render(g: Graphics) {
    if (errorState) {
      g.setColor(new Color(1f, 0f, 0f))
      g.drawString(errorMessage, 300, 300)
      return
    }

    if (inReadyRoom) {
      renderSky(g)
      readyRoom.render(g)
    }
    else { 
      if (ground.initialised) {
        renderSky(g)
      }

      g.scale(Main.GAME_WINDOW_RATIO, Main.GAME_WINDOW_RATIO)

      if (ground.initialised && null != groundImage) {
        particleSystem.render()
        ground.render(g, groundImage)
      }
      
      projectiles.values.foreach(_.render(g))
      explosions.foreach        (_.render(g))
      players.values.foreach    (_.render(g))
      tanks.foreach             ((tank) => if (null != tank) {tank.render(g)})
    }

    g.resetTransform
    g.setColor(new Color(1f, 1f, 1f))
    g.drawString("Ping: " + latency, 735, 575)

    chat.render(g)
  }
  
  def renderSky(g : Graphics) {
    if (null != skyImage) {
      g.drawImage(skyImage, 0, 0)
    }
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
      case Commands.ERROR        => serverError
      case Commands.GROUND       => loadGround
      case Commands.PING         => latency = System.currentTimeMillis - lastPing
      case Commands.TANKS        => processUpdate
      case Commands.PROJECTILE   => loadProjectile
      case Commands.PROJECTILES  => loadProjectiles
      case Commands.EXPLOSION    => loadExplosion
      case Commands.PLAYERS      => loadPlayers
      case Commands.READY_ROOM   => processReadyRoomUpdate
      case Commands.IMAGE_SET    => processImageSetUpdate
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

    previousProjectileUpdate = currentProjectileUpdate
    currentProjectileUpdate = System.currentTimeMillis

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
    val e = new Explosion(this)
    e.loadFrom(explosionArray)
    explosions += e
  }

  def loadPlayers = {
    val playersArray = new Array[Byte](data.remaining)
    data.get(playersArray)
    val playerDataList = Operations.fromByteArray[List[Array[byte]]](playersArray)

    for (player <- players.values) { player.updated = false }

    for (playerData <- playerDataList) {
      val p = new Player
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

  def serverError {
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    val message = Operations.fromByteArray[String](byteArray)

    error("Server Error: " + message)
  }
  
  def processUpdate {
    if (inReadyRoom) {
      if (null != me && null != me.tank) {
        me.tank.gun.reset
      }
      inReadyRoom = false
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
          val t = new Tank(this)
          t.create(0)
          t.loadFrom(tankData)
          players(t.id).tank = t
        }
      }
    }
  }
  
  def processReadyRoomUpdate {
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    val items = Operations.fromByteArray[Array[(Byte, Short)]](byteArray)

    for ((value, number) <- items) {
      me.items.put(Items(value), number)
    }

    if (!inReadyRoom) {
      inReadyRoom = true
    }
  }
  
  def processImageSetUpdate {
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)

    imageSetIndex = Operations.fromByteArray[Int](byteArray)
    
    groundImage = new Image("media/ground/" + imageSetIndex.toString + ".jpg")
    skyImage = new Image("media/sky/" + imageSetIndex.toString + ".jpg")

    // force groundImage.init() (which is private) to be called. I know, wtf.
    // I don't get it, why don't we need to do this for skyImage too?  What? - N
    groundImage.toString
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
    send(byteToArray(Commands.HELLO) ++ Operations.toByteArray((name, Main.VERSION)))
  }

  def sendPing = {
    send(byteToArray(Commands.PING))
  }

  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }
}
