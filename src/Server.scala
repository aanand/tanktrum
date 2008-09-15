import org.newdawn.slick._
import net.phys2d

import java.nio.channels._
import java.nio._
import java.net._
import java.util.Random

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import sbinary.Operations
import sbinary.Instances._

class Server(port: Int) extends Session(null) {
  val TANK_BROADCAST_INTERVAL = 25 //milliseconds
  val PROJECTILE_BROADCAST_INTERVAL = 50
  val PLAYER_BROADCAST_INTERVAL = 1000
  val READY_ROOM_BROADCAST_INTERVAL = 500
  val MAX_PLAYERS = 6

  var nextTankColorIndex = 0
  
  var playerID: Byte = -1

  var channel: DatagramChannel = _
  val players = new HashMap[SocketAddress, Player]
  val data = ByteBuffer.allocate(1000)
  val rand = new Random()
  
  var timeToTankUpdate = TANK_BROADCAST_INTERVAL
  var timeToProjectileUpdate = PROJECTILE_BROADCAST_INTERVAL
  var timeToPlayerUpdate = PLAYER_BROADCAST_INTERVAL
  var timeToReadyRoomUpdate = READY_ROOM_BROADCAST_INTERVAL

  var inReadyRoom = true
  
  val tankSequence = new Sequence
  val projectileSequence = new Sequence
  val groundSequence = new Sequence

  /**
   * Called to start the server.
   */
  override def enter() = {
    super.enter()

    ground.buildPoints()
    
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    channel.configureBlocking(false)
  }
  
  /**
   * Called to stop the server.
   */
  override def leave() = {
    super.leave()

    channel.socket.close()
    channel.disconnect()
  }
  
  override def tanks = {
    players.values.map(player => player.tank)
  }

  /**
   * Updates the server, processing physics and sending updates if it is time
   * to.
   */
  override def update(delta: Int) = {
    if (!inReadyRoom) {
      world.step(delta/1000f)
    }

    super.update(delta)
    checkTimeouts()

    if (inReadyRoom) {
      if (players.size > 1 && players.values.forall(player => player.ready)) {
        startRound
        //If all players are ready then we start the game.
      }

      timeToReadyRoomUpdate -= delta
      if (timeToReadyRoomUpdate < 0) {
        broadcastReadyRoom
        broadcastPlayers
        timeToReadyRoomUpdate = READY_ROOM_BROADCAST_INTERVAL
      }
    }
    else {
      if (players.values.toList.filter(player => player.tank.isAlive).size <= 1) {
        endRound
      }
      timeToTankUpdate -= delta

      if (timeToTankUpdate < 0) {
        broadcastTanks
        timeToTankUpdate = TANK_BROADCAST_INTERVAL
      }

      timeToProjectileUpdate -= delta

      if (timeToProjectileUpdate < 0) {
        broadcastProjectiles
        timeToProjectileUpdate = PROJECTILE_BROADCAST_INTERVAL
      }

      timeToPlayerUpdate -= delta

      if (timeToPlayerUpdate < 0) {
        broadcastPlayers
        timeToPlayerUpdate = PLAYER_BROADCAST_INTERVAL
      }
    }

    data.clear
    val addr = channel.receive(data)
    if (addr != null) {
      data.limit(data.position)
      data.rewind
      val command = data.get.toChar

      if (command == Commands.HELLO) {
        addPlayer(addr)
      }
      else {
        if (players.isDefinedAt(addr)) {
          processCommand(command, addr)
        }
      }
    }
  }

  /***
   * Add methods.  These all add create an instance of a game object and add it to a collection to be tracked by the server.
   */
  override def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double, projectileType: ProjectileTypes.Value) = {
    val p = super.addProjectile(tank, x, y, angle, speed, projectileType)
    broadcast(projectileData(p))
    p
  }

  override def addExplosion(x: Float, y: Float, radius: Float, projectile: Projectile) {
    val e = new Explosion(x, y, radius, this, projectile)
    explosions += e
    broadcastExplosion(e)
  }

  def endRound {
    world = createWorld
    for (projectile <- projectiles) {
      removeProjectile(projectile)
    }
    projectiles = List[Projectile]()

    for (explosion <- explosions) {
      removeExplosion(explosion)
    }
    explosions = new HashSet[Explosion]

    bodies = new HashMap[phys2d.raw.Body, Collider]
    for (player <- players.values) {
      player.money += 10
      player.ready = false
    }
    inReadyRoom = true;
    ground = new Ground(this, WIDTH, HEIGHT)
    ground.buildPoints
    for (player <- players.values) {
      val oldTank = player.tank
      player.tank.remove
      player.tank = createTank(player.id)
      player.tank.player = player //Oh no.
      if (oldTank.isAlive) { player.gun.ammo = oldTank.gun.ammo }
    }
  }
  
  def startRound {
    println("Starting game.")
    inReadyRoom = false
    broadcastGround
  }

  def addPlayer(addr: SocketAddress) {
    if (players.size >= MAX_PLAYERS) {
      sendFull(addr)
      return
    }

    if (!players.isDefinedAt(addr)) {
      val nameArray = new Array[byte](data.remaining())
      data.get(nameArray)
      val name = new String(nameArray)
      broadcastChat(name + " has joined the game.")
      println(name + " has joined the game.")

      findNextID
      val tank = createTank(playerID)
      val player = new Player(tank, name, playerID)
      tank.player = player
      players.put(addr, player)

      broadcastPlayers
      if (!inReadyRoom) {
        sendGround(addr)
        tank.health = 0
      }
    }
  }

  def createTank(id: Byte) = {
    val tank = new Tank(this, id)
    var x = rand.nextFloat * (Main.WIDTH - tank.WIDTH * 2) + tank.WIDTH
    while (tanks.exists(tank => {x > tank.x - tank.WIDTH && x < tank.x + tank.WIDTH})) {
      x = rand.nextFloat * (Main.WIDTH - tank.WIDTH * 2) + tank.WIDTH
    }
    tank.create(x)
    tank
  }

  /**
   * Check each player to see if they have timed out or not.  Remove them if they have.
   */
  def checkTimeouts() = {
    for (addr <- players.keys) {
      if (players(addr).timedOut) {
        broadcastChat(players(addr).name + " timed out.")
        println(players(addr).name + " timed out.")
        players(addr).tank.remove
        players -= addr
      }
    }
  }

  /**
   * Finds the next available player id.
   * TODO: Decide if this should be the lowest possible id or the next
   * available after the last used one. (it's currently the next available)
   */
  def findNextID {
    playerID = ((playerID + 1) % MAX_PLAYERS).toByte
    if (players.values.exists(player => { player.id == playerID })) {
      findNextID
    }
  }
  
  def processCommand(command: char, addr: SocketAddress) {
    val player = players(addr)
    player.resetTimeout
    command match {
      case Commands.PING                   => sendPong(addr)
                                           
      case Commands.MOVE_LEFT              => player.tank.thrust = -1 
      case Commands.STOP_MOVE_LEFT         => player.tank.thrust = 0 
      case Commands.MOVE_RIGHT             => player.tank.thrust = 1 
      case Commands.STOP_MOVE_RIGHT        => player.tank.thrust = 0 
      case Commands.JUMP                   => player.tank.lift = -1 
      case Commands.STOP_JUMP              => player.tank.lift = 0 
                                           
      case Commands.AIM_CLOCKWISE          => player.gun.angleChange = 1 
      case Commands.STOP_AIM_CLOCKWISE     => player.gun.angleChange = 0 
      case Commands.AIM_ANTICLOCKWISE      => player.gun.angleChange = -1 
      case Commands.STOP_AIM_ANTICLOCKWISE => player.gun.angleChange = 0 
                                           
      case Commands.POWER_UP               => player.gun.powerChange = 1 
      case Commands.STOP_POWER_UP          => player.gun.powerChange = 0 
      case Commands.POWER_DOWN             => player.gun.powerChange = -1 
      case Commands.STOP_POWER_DOWN        => player.gun.powerChange = 0 
                                           
      case Commands.START_FIRE             => player.gun.firing = true 
      case Commands.STOP_FIRE              => player.gun.firing = false 
      case Commands.CYCLE_WEAPON           => player.gun.cycleWeapon() 
                                           
      case Commands.READY                  => player.ready = true; broadcastPlayers 
      case Commands.BUY                    => handleBuy(player) 
                                           
      case Commands.CHAT_MESSAGE           => handleChat(player) 
      case _                               => println("Warning: Server got unknown command: " + command.toByte)}
  }

  def handleChat(player: Player) = {
    val messageArray = new Array[byte](data.remaining)
    data.get(messageArray)
    val message = player.name + ": " + Operations.fromByteArray[String](messageArray)
    println("Broadcasting chat message: \"" + message+ "\"")
    broadcastChat(message)
  }

  def handleBuy(player: Player) = {
    val itemArray = new Array[byte](data.remaining)
    data.get(itemArray)
    val item = Operations.fromByteArray[Byte](itemArray)
    if (inReadyRoom) {
      player.buy(Items.items(Items(item)))
    }
  }

  /***
   * Serialisation methods.  These all return byte arrays which can be sent to
   * the client as an update.
   */
  def tankPositionData = {
    val movedPlayers = players.values.filter (p => {
      val changed = p.tank.currentValues != p.tank.previousValues
      p.tank.previousValues = p.tank.currentValues
      changed
    })
    val tankDataList = movedPlayers.map(p => (p.tank.id, p.tank.serialise)).toList
    byteToArray(Commands.TANKS) ++ Operations.toByteArray((tankSequence.next.toShort, tankDataList))
  }

  def playerData = {
    val playerDataList = players.values.map(p => p.serialise).toList
    byteToArray(Commands.PLAYERS) ++ Operations.toByteArray(playerDataList)
  }

  def projectileData(p: Projectile) = {
    byteToArray(Commands.PROJECTILE) ++ p.serialise
  }

  def projectilesData() = {
    val projectileDataList = projectiles.map(p => p.serialise).toList

    byteToArray(Commands.PROJECTILES) ++ Operations.toByteArray((projectileSequence.next, projectileDataList))
  }


  /***
   * Broadcast methods.  These send an update to all clients.
   */
  def broadcastTanks {
    broadcast(tankPositionData)
  }

  def broadcastProjectiles {
    broadcast(projectilesData)
  }

  def broadcastExplosion(e: Explosion) {
    broadcast(byteToArray(Commands.EXPLOSION) ++ e.serialise)
  }

  def broadcastFrags() {
  }

  def broadcastChat (message: String) {
    broadcast(byteToArray(Commands.CHAT_MESSAGE) ++ Operations.toByteArray(message))
  }
  
  def broadcastGround() = {
    broadcast(byteToArray(Commands.GROUND) ++ ground.serialise(groundSequence.next))
  }

  def broadcastPlayers = {
    for (addr <- players.keys) {
      players(addr).me = true
      send(playerData, addr)
      players(addr).me = false
    }
  }
  
  def broadcastReadyRoom {
    broadcast(byteToArray(Commands.READY_ROOM))
  }


  /***
   * Send methods.  These send a command to a single address.
   */
  def sendPong(addr: SocketAddress) = {
    send(byteToArray(Commands.PING), addr)
  }

  def sendFull(addr: SocketAddress) = {
    send(byteToArray(Commands.SERVER_FULL), addr)
  }

  def sendGround(addr: SocketAddress) = {
    send(byteToArray(Commands.GROUND) ++ ground.serialise(groundSequence.seq), addr)
  }
  

  /**
   * Sends the provided byte array to all clients.
   */
  def broadcast(data : Array[byte]) {
    for (addr <- players.keys) {
      send(data, addr)
    }
  }

  /**
   * Sends the provided byte array to a single client.
   */
  def send(data: Array[byte], addr: SocketAddress) = {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
