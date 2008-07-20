import org.newdawn.slick._

import java.nio.channels._
import java.nio._
import java.net._
import java.util.Random
import java.util.Date

import scala.collection.mutable.HashMap

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
    sendUpdate()

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

  def sendUpdate() = {
    if (new Date().getTime > lastUpdate.getTime + UPDATE_PERIOD){
      for (addr <- players.keys) {
        for (tank <- tanks) {
          sendTank(addr, tank)
        }
      }
      lastUpdate =  new Date()
    }
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

      //TODO: Track player ids.
      players.put(addr, new Player(createTank, name, 0))

      if (ground.initialised) {
        println("Sending ground to " + players(addr).getName)
        sendGround(ground.serialise(), addr)
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
    val loc = rand.nextFloat * container.getWidth
    tank.create(loc, new Color(1.0f, 0.0f, 0.0f), tanks.length)
    tank.reposition
    tanks += tank
    tank
  }

  def processCommand(command: char, addr: SocketAddress) {
    command match {
      case Commands.PING => {
        players(addr).resetTimeout
        sendPong(addr)
      }
    }
  }

  /** Send the ground array, preceded by something telling the client that this
    * is a ground array. (I guess scala does some kind of javadoc thing like this?)
    */
  def sendGround(groundData: Array[byte], addr: SocketAddress) = {
    send(charToByteArray(Commands.GROUND) ++ groundData, addr)
  }

  def broadcastDamageUpdate(tank : Tank, damage : Int) {
    println("broadcasting damage update: " + tank.toString + ", " + damage.toString)
  }

  def sendPong(addr: SocketAddress) = {
    send(charToByteArray(Commands.PING), addr)
  }

  def sendTank(addr: SocketAddress, tank: Tank) {
    send(charToByteArray(Commands.TANK) ++ tank.serialise, addr)
  }

  def send(data: Array[byte], addr: SocketAddress) = {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
