package client

import shared._

import scala.collection.mutable.LinkedList
import java.nio.channels._
import java.nio._
import java.net._
import sbinary.Operations
import sbinary.Instances._

class ServerList(game: Game) extends Menu(List()) with Session {
  super.hide

  val serverHostname = Config("metaServer.hostname")
  val serverPort = Config("metaServer.port").toInt
  
  val filter = new MenuEditable("", 32) {
    override def keyPressed(key : Int, char : Char, menu : Menu) {
      super.keyPressed(key, char, menu)
      rebuildMenu
    }
  }
  
  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  
  var serverList: List[(String, MenuItem)] = List()

  var userName: String = _

  def show(userName: String) {
    super.show

    this.userName = userName

    serverList = List()

    channel = DatagramChannel.open()
    try {
      channel.connect(new InetSocketAddress(serverHostname, serverPort))
      channel.configureBlocking(false)
      sendCommand(Commands.REQUEST_SERVERS)
    }
    catch {
      case e: Exception => { 
        e.printStackTrace()
        return
      }
    }
  }

  override def hide() {
    game.menu.show
  }

  def update() {
    if (!channel.isConnected) return
    data.clear
    if (channel.receive(data) != null) {
      data.limit(data.position)
      data.rewind
      val command = data.get.toChar
      if (command == Commands.SERVER_INFO) {
        val serverArray = new Array[byte](data.remaining)
        data.get(serverArray)
        val (address, port) = Operations.fromByteArray[(String, Int)](serverArray)
        addServer(address, port)
      }
    }
  }

  def connect(address: String, port: Int) {
    if (userName != null) {
      super.hide
      game.startClient(address, port, userName)
    }
  }

  def addServer(address: String, port: Int) {
    serverList = serverList + (address + ":" + port, new MenuCommand(Unit => connect(address, port)))
    rebuildMenu
  }

  def rebuildMenu() {
    val list = ("Filter", filter) :: serverList.filter(item => item._1.indexOf(filter.value) != -1)
    tree = buildTree(list)
  }

  def sendCommand(command: Byte) {
    send(byteToArray(command))
  }
  
  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }
}
