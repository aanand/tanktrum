import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._
import java.util.Date

class Client (hostname: String, port: Int, name: String) extends Session {
  val PING_PERIOD = 1000
  val SERVER_TIMEOUT = 10000

  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  var lastPing = new Date()
  var lastPong = new Date()

  override def enter(container: GameContainer) = {
    super.enter(container)
    channel = DatagramChannel.open()
    channel.connect(new InetSocketAddress(hostname, port))
    channel.configureBlocking(false)

    sendHello
  }

  override def update(container: GameContainer, delta: Int) {
    super.update(container, delta)
    ping
    checkTimeout

    data.rewind
    if (channel.receive(data) != null) {
      data.rewind
      val command = data.get.toChar
      processCommand(command)
    }
  }

  def processCommand(command: Char) {
    command match {
      case Commands.GROUND => {loadGround}
      case Commands.PING   => {resetTimeout}
    }
  }
  
  def ping = {
    if (new Date().getTime - lastPing.getTime > PING_PERIOD) {
      sendPing
      lastPing = new Date()
    }
  }

  def checkTimeout = {
    if (new Date().getTime - lastPong.getTime > SERVER_TIMEOUT) {
      println("Connection timed out.")
      super.leave()
    }
  }

  def resetTimeout = {
    lastPong = new Date
  }

  def loadGround = {
    val groundArray = new Array[byte](data.remaining)
    data.get(groundArray)
    ground.loadFrom(groundArray)
  }

  def sendHello = {
    send(charToByteArray(Commands.HELLO) ++ name.getBytes)
  }

  def sendPing = {
    send(charToByteArray(Commands.PING))
  }

  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }
}
