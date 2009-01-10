package metaserver

import shared._

class Server(val name: String, val hostname: String, val port: Int, var players: Int, var maxPlayers: Int) {
  val timeOut = Config("metaServer.serverTimeout").toLong
  val updated = System.currentTimeMillis

  def expired = System.currentTimeMillis - updated > timeOut
}
