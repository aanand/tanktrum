import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._
import java.util.Date

import sbinary.Operations
import sbinary.Instances._

class Client (hostname: String, port: Int, name: String, container: GameContainer) extends Session(container) {
  val PING_PERIOD = 1000
  val SERVER_TIMEOUT = 10000

  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  var lastPing = new Date()
  var lastPong = new Date()

  override def enter() = {
    super.enter()
    channel = DatagramChannel.open()
    channel.connect(new InetSocketAddress(hostname, port))
    channel.configureBlocking(false)

    sendHello
  }

  override def update(delta: Int) {
    super.update(delta)
    ping
    checkTimeout

    data.rewind
    if (channel.receive(data) != null) {
      data.rewind
      val command = data.get.toChar
      processCommand(command)
    }
  }
  
  override def broadcastUpdate() {}

  def processCommand(command: Char) {
    command match {
      case Commands.GROUND => {loadGround}
      case Commands.PING   => {resetTimeout}
      case Commands.UPDATE => {processUpdate}
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
  
  def processUpdate = {
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    
    val tankDataList : List[Array[byte]] = Operations.fromByteArray[List[Array[byte]]](byteArray)

    tanks = tankDataList.map(tankData => {
      val t = new Tank(this)
      t.loadFrom(tankData)
      t
    })
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
