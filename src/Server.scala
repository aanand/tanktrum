import org.newdawn.slick
import java.nio.channels._
import java.nio._
import java.net._

class Server(port: int) extends Session {
  var channel: DatagramChannel = _

  override def enter(container : slick.GameContainer) {
    super.enter(container)
    
    ground = new Ground(this, container.getWidth(), container.getHeight())
    ground.buildPoints()
    ground.serialise()
    
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    
    //var data = ByteBuffer.allocate(100)
    //channel.receive(data)
    //println(new String(data.array))
  }
}
