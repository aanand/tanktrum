import org.newdawn.slick._

import java.nio.channels._
import java.nio._
import java.net._
import java.util.Random
import java.util.Date

import scala.collection.mutable.HashMap

import sbinary.Operations
import sbinary.Instances._

class Server(port: Int, userName: String, container: GameContainer) extends Session(container) {
  var channel: DatagramChannel = _
  val players = new HashMap[SocketAddress, Player]
  val data = ByteBuffer.allocate(1000)
  val rand = new Random()

  var lastUpdate = new Date()
  val UPDATE_PERIOD = 100

  override def enter() = {
    super.enter()

    ground.buildPoints()
    
    me = new Player(createTank, userName, 0)
    
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    channel.configureBlocking(false)
  }

  override def update(delta: Int) = {
    super.update(delta)
    checkTimeouts()

    data.rewind()
    val addr = channel.receive(data)
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

  override def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double) = {
    val p = super.addProjectile(tank, x, y, angle, speed)
    broadcast(projectileData(p))
    p
  }
  
  def broadcastUpdate() {
    broadcast(tankPositionData)
  }
  
  def tankPositionData = {
    val tankDataList : List[Array[Byte]] = me.tank.serialise :: players.values.map(p => p.tank.serialise).toList

    byteToArray(Commands.TANKS) ++ Operations.toByteArray(tankDataList)
  }

  def projectileData(p: Projectile) = {
    byteToArray(Commands.PROJECTILE) ++ p.serialise
  }

  def checkTimeouts() = {
    for (addr <- players.keys) {
      if (players(addr).timedOut) {
        println(players(addr).getName + " timed out.")
        players(addr).tank.die
        players -= addr
      }
    }
  }

  /**
    * Adds a player, returns false if they already existed.
    */
  def addPlayer(addr: SocketAddress) = {
    if (!players.isDefinedAt(addr)) {
      val nameArray = new Array[byte](data.remaining())
      data.get(nameArray)
      val name = new String(nameArray)
      println("Adding player: " + name)

      players.put(addr, new Player(createTank, name, 0))

      if (ground.initialised) {
        println("Sending ground to " + players(addr).getName)
        sendGround(ground.serialise(), addr)
        send(tankPositionData, addr)
      }
      true
    }
    else {
      false
    }
  }
  
  def createTank = {
    println("Creating a tank.")
    val tank = new Tank(this)
    val loc = rand.nextFloat * (container.getWidth - 200) + 100
    tank.create(loc, new Color(1.0f, 0.0f, 0.0f))
    tank.reposition
    tanks += tank
    tank
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
    }
  }

  /** Send the ground array, preceded by something telling the client that this
    * is a ground array. (I guess scala does some kind of javadoc thing like this?)
    */
  def sendGround(groundData: Array[byte], addr: SocketAddress) = {
    send(byteToArray(Commands.GROUND) ++ groundData, addr)
  }

  def broadcastDamageUpdate(tank : Tank, damage : Int) {
    println("broadcasting damage update: " + tank.toString + ", " + damage.toString)
  }

  def sendPong(addr: SocketAddress) = {
    send(byteToArray(Commands.PING), addr)
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
