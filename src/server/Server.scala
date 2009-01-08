package server

import shared._

import java.nio.channels._
import java.nio._
import java.net._
import java.util.Random
import java.util.Date

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.actors.Actor

import sbinary.Operations
import sbinary.Instances._

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

class Server(port: Int) extends Session with Actor with ContactListener  {
  val TANK_BROADCAST_INTERVAL       = Config("server.tankBroadcastInterval").toInt
  val PROJECTILE_BROADCAST_INTERVAL = Config("server.projectileBroadcastInterval").toInt
  val PLAYER_BROADCAST_INTERVAL     = Config("server.playerBroadcastInterval").toInt
  val READY_ROOM_BROADCAST_INTERVAL = Config("server.readyRoomBroadcastInterval").toInt
  val MAX_PLAYERS                   = Config("server.maxPlayers").toInt
  
  val tick = Config("game.logicUpdateInterval").toInt
  
  var nextTankColorIndex = 0
  
  var playerID: Byte = -1
  var nextProjectileId = 0

  var channel: DatagramChannel = _
  val players = new HashMap[SocketAddress, Player]
  val data = ByteBuffer.allocate(10000)
  val rand = new Random()
  
  var timeToTankUpdate = TANK_BROADCAST_INTERVAL
  var timeToProjectileUpdate = PROJECTILE_BROADCAST_INTERVAL
  var timeToPlayerUpdate = PLAYER_BROADCAST_INTERVAL
  var timeToReadyRoomUpdate = READY_ROOM_BROADCAST_INTERVAL

  var supposedRunTime = 0
  var numTankUpdates = 0
  var startTime: Long = 0

  var inReadyRoom = true
  
  val imageSetCount = 7
  var imageSetIndex = rand.nextInt(imageSetCount) + 1
  
  var endRoundTimer = 0
  val endRoundRunnoffTime = Config("server.roundRunoffTime").toInt

  val tankSequence = new Sequence
  val projectileSequence = new Sequence
  val groundSequence = new Sequence
  
  var world = createWorld
  var bodies = new HashMap[Body, GameObject]
  var ground: Ground = _
  var projectiles = new HashMap[Int, Projectile]
  var explosions = new HashSet[Explosion]
  def tanks = players.values.map(player => player.tank)
  
  def act {
    println("Server started on port " + port + ".")
    var time = System.currentTimeMillis

    while (true) {
      while (mailboxSize > 0) {
        receive {
          case 'enter =>
            enter
            reply(true)
          case 'leave =>
            leave
            reply(true)
            exit()
          case _ =>
            println("Server: Received unknown message")
            reply(true)
        }
      }
      
      if (active) {
        val newTime = System.currentTimeMillis
        val delta = (newTime - time)
        time = newTime

        update(delta.toInt)
        
        val updateTime = time - System.currentTimeMillis
        if (tick-updateTime > 0) {
          Thread.sleep(tick-updateTime)
        }
      }
    }
  }

  protected def enter() {
    active = true
    ground = new Ground(this, Main.GAME_WIDTH.toInt, Main.GAME_HEIGHT.toInt)
    ground.buildPoints()

    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    channel.configureBlocking(false)
  }
  
  protected def leave() = {
    active = false
    channel.socket.close()
    channel.disconnect()
  }
  

  protected def update(delta: Int) = {
    supposedRunTime += delta
    if (!inReadyRoom) {
      world.step(delta/1000f, 10)
    }

    var body = world.getBodyList

    while (null != body) {
      if (null != body.getUserData) { 
        body.wakeUp
      }
      body = body.getNext
    }

    ground.update(delta)
    for (p <- projectiles.values) {
      p.update(delta)
    }
    for (tank <- tanks) {
      if (null != tank) { tank.update(delta) }
    }
    for (e <- explosions) {
      e.update(delta)
    }

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
        endRoundTimer += delta
        if (endRoundTimer > endRoundRunnoffTime) {
          endRound
        }
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
        if (players.isDefinedAt(addr) || command == Commands.PING) {
          processCommand(command, addr)
        }
      }
    }
  }

  def createWorld = {
    val gravity = new Vec2(0.0f, Config("physics.gravity").toFloat)
    val bounds = new AABB(new Vec2(-Main.GAME_WIDTH, -Main.GAME_HEIGHT),
                          new Vec2(2*Main.GAME_WIDTH, 2*Main.GAME_HEIGHT))

    val newWorld = new World(bounds, gravity, false)

    newWorld.setContactListener(this)
    newWorld
  }

  
  def createBody(obj: GameObject, bodyDef: BodyDef) = {
    val body = world.createBody(bodyDef)
    if (null != body) {
      bodies.put(body, obj)
      body.setUserData(obj)
    }
    body
  }

  def removeBody(body: Body) {
    world.destroyBody(body)
    bodies -= body
  }

  /***
   * Add methods.  These all add create an instance of a game object and add it to a collection to be tracked by the server.
   */
  def addProjectile(tank: Tank, x: Float, y: Float, angle: Float, speed: Float, projectileType : ProjectileTypes.Value): Projectile = {
    val radians = Math.toRadians(angle-90)
    
    val velocity = new Vec2((speed * Math.cos(radians)).toFloat, (speed * Math.sin(radians)).toFloat)
    velocity.addLocal(tank.velocity)

    val position = new Vec2(x.toFloat, y.toFloat)
    
    var p: Projectile = Projectile.create(this, tank, projectileType)

    p.body.setXForm(position, 0f)
    p.body.setLinearVelocity(tank.velocity.add(velocity))
    
    addProjectile(p)
    broadcast(projectileData(p))
    p
  }

  def addProjectile(p: Projectile) = {
    p.id = nextProjectileId
    projectiles.put(nextProjectileId, p)
    nextProjectileId += 1
    p
  }

  def removeProjectile(p : Projectile) {
    p.onRemove
    projectiles -= p.id
  }

  def addExplosion(x: Float, y: Float, radius: Float, projectile: Projectile, damageFactor: Float) {
    val e = new Explosion(x, y, radius, this, projectile, damageFactor)
    explosions += e
    broadcastExplosion(e)
  }

  def removeExplosion(e: Explosion) {
    removeBody(e.body)
    explosions -= e
  }

  def endRound {
    val runTime = (new Date().getTime - startTime).toFloat
    
    val prefix = this.getClass.getName + ": "
    
    println(prefix + "runTime = " + runTime/1000)
    println(prefix + "numTankUpdates = " + numTankUpdates)
    
    if (numTankUpdates > 0) {
      val targetTankUpdateRate = 1000f / TANK_BROADCAST_INTERVAL
      val actualTankUpdateRate = numTankUpdates.toFloat/runTime * 1000
      val error = (actualTankUpdateRate - targetTankUpdateRate) / targetTankUpdateRate * 100
      
      println(prefix + "avg tank update interval = " + runTime/numTankUpdates)
      println(prefix + "target tank update = " + targetTankUpdateRate + " updates/sec")
      println(prefix + "actual tank update rate = " + actualTankUpdateRate + " updates/sec")
      println(prefix + "update rate error = " + error + "%")
    }
    
    val error = (supposedRunTime - runTime).toFloat / runTime * 100
    
    println(prefix + "supposedRunTime = " + supposedRunTime.toFloat/1000)
    println(prefix + "delta error = " + error + "%")
 
    world = createWorld
    
    projectiles = new HashMap[Int, Projectile]
    explosions = new HashSet[Explosion]
    bodies = new HashMap[Body, GameObject]

    for (player <- players.values) {
      player.money += 10
      player.ready = false
    }
    
    inReadyRoom = true
    
    imageSetIndex += 1
    
    if (imageSetIndex > imageSetCount) {
      imageSetIndex -= imageSetCount
    }
    
    broadcast(imageSetData)
    
    ground = new Ground(this, Main.GAME_WIDTH.toInt, Main.GAME_HEIGHT.toInt)
    ground.buildPoints
    for (player <- players.values) {
      val oldTank = player.tank
      //player.tank.remove
      player.tank = createTank(player.id)
      player.tank.player = player //Oh no.
      if (oldTank.isAlive) { 
        player.gun.ammo = oldTank.gun.ammo 
        player.tank.purchasedJumpFuel = oldTank.purchasedJumpFuel
        player.tank.corbomite = oldTank.corbomite
      }
    }
  }
  
  def startRound {
    startTime = System.currentTimeMillis
    supposedRunTime = 0
    numTankUpdates = 0
    
    println("Starting game.")
    inReadyRoom = false
    broadcastGround
    endRoundTimer = 0
  }

  def addPlayer(addr: SocketAddress) {
    if (players.size >= MAX_PLAYERS) {
      sendError(addr, "Server full.")
      return
    }

    if (!players.isDefinedAt(addr)) {
      val helloArray = new Array[byte](data.remaining())
      data.get(helloArray)
      val (name, version) = Operations.fromByteArray[(String, Int)](helloArray)
      
      if (version > Main.VERSION) {
        sendError(addr, "Client version (" + version + ") is newer than server version (" + Main.VERSION + ").")
      }
      else if (version < Main.VERSION) {
        sendError(addr, "Client version (" + version + ") is older than server version (" + Main.VERSION + ").")
      }
      else {
        broadcastChat(name + " has joined the game.")
        println(name + " has joined the game.")

        findNextID
        
        val tank = createTank(playerID)
        val player = new Player(tank, name, playerID)
        tank.player = player
        players.put(addr, player)

        broadcastPlayers

        send(imageSetData, addr)

        if (!inReadyRoom) {
          sendGround(addr)
          tank.destroy = true
        }
      }
    }
  }

  def createTank(id: Byte) = {
    val tank = new Tank(this, id)
    var x = rand.nextFloat * (Main.GAME_WIDTH - tank.WIDTH * 2) + tank.WIDTH
    while (tanks.exists(tank => {x > tank.x - tank.WIDTH && x < tank.x + tank.WIDTH})) {
      x = rand.nextFloat * (Main.GAME_WIDTH - tank.WIDTH * 2) + tank.WIDTH
    }

    if (inReadyRoom) {
      ground.flatten(x)
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

  def leader = {
    var leadPlayer: Player = null
    for (player <- players.values) {
      if (null == leadPlayer || player.score > leadPlayer.score) {
        leadPlayer = player
      }
    }
    leadPlayer
  }

  /**
   * Finds the lowest available player id.
   */
  def findNextID {
    playerID = 0
    while (players.values.exists(player => { player.id == playerID })) {
      playerID = (playerID+1).toByte
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
                                           
      case Commands.TANK_UPDATE            => handleTankUpdate(player)
                                           
      case Commands.START_FIRE             => player.gun.firing = true 
      case Commands.STOP_FIRE              => player.gun.firing = false 
      case Commands.CYCLE_WEAPON           => player.gun.cycleWeapon() 
                                           
      case Commands.READY                  => player.ready = !player.ready; broadcastPlayers 
      case Commands.BUY                    => handleBuy(player) 
                                           
      case Commands.CHAT_MESSAGE           => handleChat(player) 
      
      case Commands.GOODBYE                => handleGoodbye(addr)
      case _                               => println("Warning: Server got unknown command: " + command.toByte)}
  }

  def handleTankUpdate(player: Player) = {
    val tankArray = new Array[byte](data.remaining)
    data.get(tankArray)
    player.tank.loadFrom(tankArray)
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

  def handleGoodbye(addr: SocketAddress) = {
    val player = players(addr)
    println(player.name + " has left the game.")
    broadcastChat(player.name + " has left the game.")
    player.tank.remove
    players -= addr
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
    val projectileDataArray = new Array[Array[byte]](projectiles.size)
    projectiles.values.map(p => p.serialise).copyToArray(projectileDataArray, 0)

    byteToArray(Commands.PROJECTILES) ++ Operations.toByteArray((projectileSequence.next, projectileDataArray))
  }

  def imageSetData = byteToArray(Commands.IMAGE_SET) ++ Operations.toByteArray(imageSetIndex)

  /***
   * Broadcast methods.  These send an update to all clients.
   */
  def broadcastTanks {
    broadcast(tankPositionData)
    numTankUpdates += 1
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
    for (addr <- players.keys) {
      sendReadyRoom(addr)
    }
  }


  /***
   * Send methods.  These send a command to a single address.
   */
  def sendPong(addr: SocketAddress) = {
    send(byteToArray(Commands.PING), addr)
  }

  def sendError(addr: SocketAddress, message: String) = {
    send(byteToArray(Commands.ERROR) ++ Operations.toByteArray(message), addr)
  }

  def sendGround(addr: SocketAddress) = {
    send(byteToArray(Commands.GROUND) ++ ground.serialise(groundSequence.seq), addr)
  }

  def sendReadyRoom(addr: SocketAddress) = {
    send(byteToArray(Commands.READY_ROOM) ++ Operations.toByteArray(players(addr).itemsAsArray), addr)
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

  /**
   * Contact listener callbacks:
   */
  override def add(contact: ContactPoint) {
    val a = contact.shape1.getBody()
    val b = contact.shape2.getBody()
    
    bodies(a).collide(bodies(b), contact)
    bodies(b).collide(bodies(a), contact)
  }

  override def persist(contact: ContactPoint) {
    val a = contact.shape1.getBody()
    val b = contact.shape2.getBody()
    
    bodies(a).persist(bodies(b), contact)
    bodies(b).persist(bodies(a), contact)
  }
  
  override def remove(contact: ContactPoint) {
  }

  override def result(result: ContactResult) {
  }
}
