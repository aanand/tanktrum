import org.newdawn.slick._

import java.nio.channels._
import java.nio._
import java.net._
import java.util.Random

import scala.collection.mutable.HashMap

import sbinary.Operations
import sbinary.Instances._

class Server(port: Int) extends Session(null) {
  val TANK_BROADCAST_INTERVAL = 25 //milliseconds
  val PROJECTILE_BROADCAST_INTERVAL = 100
  val PLAYER_BROADCAST_INTERVAL = 1000
  val MAX_PLAYERS = 6

  val TANK_COLORS = List(
    new Color(1f, 0f, 0f),
    new Color(0f, 1f, 0f),
    new Color(0f, 0f, 1f),
    new Color(1f, 1f, 0f),
    new Color(1f, 0f, 1f),
    new Color(0f, 1f, 1f))
  
  var nextTankColorIndex = 0
  
  var playerID: Byte = -1

  var channel: DatagramChannel = _
  val players = new HashMap[SocketAddress, Player]
  val data = ByteBuffer.allocate(1000)
  val rand = new Random()
  
  var timeToTankUpdate = TANK_BROADCAST_INTERVAL
  var timeToProjectileUpdate = PROJECTILE_BROADCAST_INTERVAL
  var timeToPlayerUpdate = PLAYER_BROADCAST_INTERVAL

  override def enter() = {
    super.enter()

    ground.buildPoints()
    
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    channel.configureBlocking(false)
  }
  
  override def leave() = {
    super.leave()

    channel.socket.close()
    channel.disconnect()
  }

  override def update(delta: Int) = {
    super.update(delta)
    checkTimeouts()

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

    data.clear()
    val addr = channel.receive(data)
    data.limit(data.position)
    data.rewind()
    if (addr != null) {
      val command = data.get().toChar

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

  override def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double, projectileType: ProjectileTypes.Value) = {
    val p = super.addProjectile(tank, x, y, angle, speed, projectileType)
    broadcast(projectileData(p))
    p
  }

  override def addExplosion(x: Float, y: Float, radius: Float) {
    val e = new Explosion(x, y, radius, this)
    explosions += e
    broadcastExplosion(e)
  }

  def tankPositionData = {
    val tankDataList = players.values.map(p => p.tank.serialise).toList
    byteToArray(Commands.TANKS) ++ Operations.toByteArray(tankDataList)
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

    byteToArray(Commands.PROJECTILES) ++ Operations.toByteArray(projectileDataList)
  }

  def checkTimeouts() = {
    for (addr <- players.keys) {
      if (players(addr).timedOut) {
        println(players(addr).name + " timed out.")
        players(addr).tank.kill
        players -= addr
      }
    }
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
      println("Adding player: " + name)

      findNextID
      val tank = createTank(playerID)
      val player = new Player(tank, name, playerID)
      tank.player = player
      players.put(addr, player)

      if (ground.initialised) {
        println("Sending ground to " + players(addr).name)
        sendGround(addr)
        send(tankPositionData, addr)
      }
      broadcastPlayers
    }
  }

  def findNextID {
    playerID = ((playerID + 1) % MAX_PLAYERS).toByte
    if (players.values.exists(player => { player.id == playerID })) {
      findNextID
    }
  }
  
  def createTank(id: Byte) = {
    println("Creating a tank.")
    val tank = new Tank(this, id)
    val loc = rand.nextFloat * (Main.WIDTH - 200) + 100
    tank.create(loc, nextTankColor)
    tanks += tank
    tank
  }
  
  def nextTankColor = {
    nextTankColorIndex += 1
    nextTankColorIndex = nextTankColorIndex % TANK_COLORS.length
    TANK_COLORS(nextTankColorIndex-1)
  }

  def processCommand(command: char, addr: SocketAddress) {
    val player = players(addr)
    command match {
      case Commands.PING => {
        player.resetTimeout
        sendPong(addr)
      }

      case Commands.MOVE_LEFT => { player.tank.thrust = -1 }
      case Commands.STOP_MOVE_LEFT => { player.tank.thrust = 0 }
      case Commands.MOVE_RIGHT => { player.tank.thrust = 1 }
      case Commands.STOP_MOVE_RIGHT => { player.tank.thrust = 0 }

      case Commands.AIM_CLOCKWISE => { player.tank.gunAngleChange = 1 }
      case Commands.STOP_AIM_CLOCKWISE => { player.tank.gunAngleChange = 0 }
      case Commands.AIM_ANTICLOCKWISE => { player.tank.gunAngleChange = -1 }
      case Commands.STOP_AIM_ANTICLOCKWISE => { player.tank.gunAngleChange = 0 }

      case Commands.POWER_UP => { player.tank.gunPowerChange = 1 }
      case Commands.STOP_POWER_UP => { player.tank.gunPowerChange = 0 }
      case Commands.POWER_DOWN => { player.tank.gunPowerChange = -1 }
      case Commands.STOP_POWER_DOWN => { player.tank.gunPowerChange = 0 }

      case Commands.FIRE => { player.tank.fire() }
      case Commands.CYCLE_WEAPON => { player.tank.cycleWeapon() }
    }
  }

  /** Send the ground array, preceded by something telling the client that this
    * is a ground array. (I guess scala does some kind of javadoc thing like this?)
    */
  def sendGround(addr: SocketAddress) = {
    send(byteToArray(Commands.GROUND) ++ ground.serialise, addr)
  }
  
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
  
  def broadcastGround() = {
    broadcast(byteToArray(Commands.GROUND) ++ ground.serialise)
  }

  def broadcastDamageUpdate(tank : Tank, damage : Int) {
    println("broadcasting damage update: " + tank.toString + ", " + damage.toString)
  }

  def broadcastPlayers = {
    broadcast(playerData)
  }

  def sendPong(addr: SocketAddress) = {
    send(byteToArray(Commands.PING), addr)
  }

  def sendFull(addr: SocketAddress) = {
    send(byteToArray(Commands.SERVER_FULL), addr)
  }
  
  def broadcast(data : Array[byte]) {
    for (addr <- players.keys) {
      send(data, addr)
    }
  }

  def send(data: Array[byte], addr: SocketAddress) = {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
