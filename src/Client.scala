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
      case Commands.TANKS => {processUpdate}
      case Commands.PROJECTILE => {loadProjectile}
    }
  }
  
  override def keyPressed(key : Int, char : Char) {
    char match {
      case 'a' => { sendCommand(Commands.MOVE_LEFT) }
      case 'd' => { sendCommand(Commands.MOVE_RIGHT) }
      case _ => {
        key match {
          case Input.KEY_LEFT  => { sendCommand(Commands.AIM_ANTICLOCKWISE) }
          case Input.KEY_RIGHT => { sendCommand(Commands.AIM_CLOCKWISE) }
          case Input.KEY_UP    => { sendCommand(Commands.POWER_UP) }
          case Input.KEY_DOWN  => { sendCommand(Commands.POWER_DOWN) }
          case Input.KEY_SPACE => { sendCommand(Commands.FIRE) }
          case _ => {}
        }
      }
    }
  }
  
  override def keyReleased(key : Int, char : Char) {
    char match {
      case 'a' => { sendCommand(Commands.STOP_MOVE_LEFT) }
      case 'd' => { sendCommand(Commands.STOP_MOVE_RIGHT) }
      case _ => {
        key match {
          case Input.KEY_LEFT  => { sendCommand(Commands.STOP_AIM_ANTICLOCKWISE) }
          case Input.KEY_RIGHT => { sendCommand(Commands.STOP_AIM_CLOCKWISE) }
          case Input.KEY_UP    => { sendCommand(Commands.STOP_POWER_UP) }
          case Input.KEY_DOWN  => { sendCommand(Commands.STOP_POWER_DOWN) }
          case _ => {}
        }
      }
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
    if (ground != null && null != ground.body) {
      removeBody(ground.body)
    }
    data.get(groundArray)
    ground.loadFrom(groundArray)
  }

  def loadProjectile = {
    val projArray = new Array[byte](data.remaining)
    data.get(projArray)
    addProjectile(tanks(0), 0, 0, 0, 0).loadFrom(projArray)
  }
  
  def processUpdate = {
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    
    val tankDataList = Operations.fromByteArray[List[Array[byte]]](byteArray)
  
    if (tanks.length == tankDataList.length) {
      for (i <- 0 until tanks.length) {
        tanks(i).loadFrom(tankDataList(i))
      }
    }
    else {
      for (tank <- tanks) {
        removeBody(tank.body)
      }
      tanks = tankDataList.map(tankData => {
        val t = new Tank(this)
        t.create(0, null)
        t.loadFrom(tankData)
        t
      })
    }
  }

  def sendCommand(command: Byte) {
    send(byteToArray(command))
  }

  def sendHello = {
    send(byteToArray(Commands.HELLO) ++ name.getBytes)
  }

  def sendPing = {
    send(byteToArray(Commands.PING))
  }

  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }
}
