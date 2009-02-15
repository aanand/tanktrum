package metaserver

import shared._

class Server(var name: String, val hostname: String, val port: Int, var players: Int, var maxPlayers: Int) {
  val timeOut = Config("metaServer.serverTimeout").toLong
  var updated = System.currentTimeMillis

  def expired = System.currentTimeMillis - updated > timeOut
  def updateTimeout = {
    updated = System.currentTimeMillis
  }

  var gotPing = false
}
