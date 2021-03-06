package metaserver

import shared._

import sbinary.Operations
import sbinary.Instances._

import scala.collection.mutable.HashMap

import java.nio.channels._
import java.nio._
import java.net._

object MetaServerMain {
  def main(args: Array[String]) {
    new MetaServer().start
  }
}

class MetaServer extends Session {
  val data = ByteBuffer.allocate(10000)
  var channel: DatagramChannel = _
  val port = Config("metaServer.port").toInt

  var servers = new HashMap[SocketAddress, Server]

  def start() {
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))
    channel.configureBlocking(false)

    println("Metaserver listening on " + port)

    while(true) {
      for (key <- servers.keys) {
        if (servers(key).expired) {
          println("Server " + servers(key).name + " timed out.")
          servers -= key
        }
      }

      data.clear
      val addr = channel.receive(data)
      if (addr != null) {
        data.limit(data.position)
        data.rewind
        val command = data.get.toChar

        if (command == Commands.REQUEST_SERVERS) {
          for (server <- servers.values) {
            if (server.gotPing) {
              send(byteToArray(Commands.SERVER_INFO) ++ Operations.toByteArray(server.name, 
                                                                               server.hostname, 
                                                                               server.port, 
                                                                               server.players, 
                                                                               server.maxPlayers), 
                   addr)
            }
          }

        }
        else if (command == Commands.STATUS_UPDATE) {
          val serverArray = new Array[byte](data.remaining)
          data.get(serverArray)
          val (name, players, maxPlayers) = Operations.fromByteArray[(String, Int, Int)](serverArray)
          val inetAddr = addr.asInstanceOf[InetSocketAddress]
          val host = inetAddr.getHostName
          val port = inetAddr.getPort

          if (servers.isDefinedAt(inetAddr)) {
            println("Updating server: " + name)
            val server = servers(addr)
            server.name = name
            server.players = players
            server.maxPlayers = maxPlayers
            server.updateTimeout
            if (!server.gotPing) {
              send(byteToArray(Commands.PING), addr)
            }
          }
          else {
            println("Adding server: " + name)
            servers.put(inetAddr, new Server(name, host, port, players, maxPlayers))
            send(byteToArray(Commands.PING), addr)
          }
        }
        else if (command == Commands.PING) {
          println("Got a ping.")
          servers(addr).gotPing = true
        }
      }
      Thread.sleep(10)
    }
  }

  def send(data: Array[byte], addr: SocketAddress) = {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
