import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._
import scala.collection.mutable.HashMap

class Server(port: Int) extends Session {
  def HELLO = 'h'
  def PING  = 'p'
  def GROUND = 'g'

  var channel: DatagramChannel = _
  val players = new HashMap[SocketAddress, Player]
  val data = ByteBuffer.allocate(1000)

  override def enter(container : GameContainer) = {
    super.enter(container)
    
    ground = new Ground(this, container.getWidth(), container.getHeight())
    ground.buildPoints()
    
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    channel.configureBlocking(false)
    
    //var data = ByteBuffer.allocate(100)
    //channel.receive(data)
    //println(new String(data.array))
  }

  override def update(container: GameContainer, delta: Int) = {
    checkTimeouts()

    data.rewind()
    val addr = channel.receive(data)
    data.rewind()
    if (addr != null) {
      val command = data.get().toChar

      if (command == HELLO) {
        addPlayer(addr)
      }
      else {
        if (players.isDefinedAt(addr)) {
          val player = players(addr)
          processCommand(command, player)
        }
      }
    }
  }

  def checkTimeouts() = {
    for (addr <- players.keys) {
      if (players(addr).timedOut) {
        println(players(addr) + " timed out.")
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

      //Whee, brackets.  Outer ones get eaten by += as a method call, then we
      //need some inner ones to pass it a tuple.  A thing on the internet
      //suggests that players += foo -> bar should work, but it doesn't seem
      //to.
      players += ((addr, new Player(new Tank(), name)))

      if (ground != null) {
        sendGround(ground.serialise(), addr)
      }
      true
    }
    else {
      false
    }
  }

  def processCommand(command: char, player: Player) {
    println(player)
    if (command == PING) {
      player.resetTimeout()
    }
  }

  /** Send the ground array, preceded by something telling the client that this
    * is a ground array. (I guess scala does some kind of javadoc thing like this?)
    */
  def sendGround(groundData: Array[byte], addr: SocketAddress) = {
    val a = new Array[byte](1)
    a(0) = GROUND.toByte
    send(a ++ groundData, addr)
  }

  def send(data: Array[byte], addr: SocketAddress) = {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
