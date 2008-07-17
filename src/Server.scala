import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._
import scala.collection.mutable.HashMap

class Server(port: Int) extends Session {
  var channel: DatagramChannel = _
  val players = new HashMap[SocketAddress, Player]

  override def enter(container : GameContainer) {
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

  override def update(container: GameContainer, delta: Int) {
    val data = ByteBuffer.allocate(1000)
    val addr = channel.receive(data)
    if (addr != null && !players.isDefinedAt(addr) && data.array()(0).toChar == 'h') {
      println("Adding player...")
      //Whee, brackets.  Outer ones get eaten by += as a method call, then we
      //need some inner ones to pass it a tuple.  A thing on the internet
      //suggests that players += foo -> bar should work, but it doesn't seem
      //to.
      players += ((addr, new Player(new Tank())))

      if (ground != null) {
        sendGround(ground.serialise(), addr)
      }
    }
  }

  /** Send the ground array, preceded by something telling the client that this
    * is a ground array. (I guess scala does some kind of javadoc thing like this?)
    */
  def sendGround(groundData: Array[byte], addr: SocketAddress) {
  }

  def send(data: Array[byte], addr: SocketAddress) {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
