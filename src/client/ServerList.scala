package client

import shared._

import org.newdawn.slick.Input

import scala.collection.mutable.LinkedList
import java.nio.channels._
import java.nio._
import java.net._
import sbinary.Operations
import sbinary.Instances._

class ServerList(game: Game) extends Menu(List()) with Session {
  hide

  val serverHostname = Config("metaServer.hostname")
  val serverPort = Config("metaServer.port").toInt
  
  val filter = new MenuEditable("", 32) {
    override def keyPressed(key: Int, char: Char, menu: Menu) {
      super.keyPressed(key, char, menu)
      rebuildMenu
    }
  }
  
  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  
  var serverList: List[(ServerItem, MenuItem)] = List()

  var userName: String = _

  def show(userName: String) {
    super.show

    this.userName = userName

    serverList = List()
    rebuildMenu

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

  override def cancel() = {
    super.cancel()
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
        val (name, address, port, players, maxPlayers) = Operations.fromByteArray[(String, String, Int, Int, Int)](serverArray)
        addServer(name, address, port, players, maxPlayers)
      }
    }
  }

  def connect(address: String, port: Int) {
    if (userName != null) {
      game.startClient(address, port, userName)
    }
  }

  def addServer(name: String, address: String, port: Int, players: Int, maxPlayers: Int) {
    val server = new ServerItem(name, address, port, players, maxPlayers)
    server.getPing

    serverList = serverList + (server, MenuCommand(Unit => connect(address, port)))
    rebuildMenu
  }

  def rebuildMenu() {
    val list = ("Filter", filter) :: serverList.filter(item => item._1.toString.indexOf(filter.value) != -1)
    tree = buildTree(list)
  }

  def sendCommand(command: Byte) {
    send(byteToArray(command))
  }
  
  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }

}
  
class ServerItem(name: String, address: String, port: Int, players: Int, maxPlayers: Int) extends Session {
  var ping = -1L
  override def toString() = {
    if (ping < 0) {
      name + "(" + address + ":" + port + ") " + players + "/" + maxPlayers
    }
    else {
      name + "(" + address + ":" + port + ") " + players + "/" + maxPlayers + " " + ping + "ms"
    }
  }

  def getPing() = {
    new Thread() {
      override def run() = {
        val data = ByteBuffer.allocate(10)
        val startTime = System.currentTimeMillis;
        val channel = DatagramChannel.open()
        
        try {
          channel.connect(new InetSocketAddress(address, port))
          channel.write(ByteBuffer.wrap(byteToArray(Commands.PING)))
          channel.receive(data)
          ping = System.currentTimeMillis - startTime;
        }
        catch {
          case e: Exception => { 
            ping = 999
          }
        }
      }
    }.start();
  }
}


