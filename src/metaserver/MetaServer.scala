package metaserver

import shared._

import sbinary.Operations
import sbinary.Instances._

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

  var servers = List[Server]()

  def start() {
    channel = DatagramChannel.open()
    channel.socket.bind(new InetSocketAddress(port))

    servers = new Server("localhost", "localhost", 10000, 5, 6) :: servers

    println("Metaserver listening on " + port)

    while(true) {
      data.clear
      val addr = channel.receive(data)
      if (addr != null) {
        data.limit(data.position)
        data.rewind
        val command = data.get.toChar

        if (command == Commands.REQUEST_SERVERS) {
          for (server <- servers) {
            send(byteToArray(Commands.SERVER_INFO) ++ Operations.toByteArray(server.name, server.port), addr)
          }
        }
      }
    }
  }

  def send(data: Array[byte], addr: SocketAddress) = {
    channel.send(ByteBuffer.wrap(data), addr)
  }
}
