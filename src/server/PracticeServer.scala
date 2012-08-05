package server

import shared._

import java.nio.channels._
import java.nio._
import java.net._

class PracticeServer(port : Int) extends Server(port, "", false) {
  override def enter() {
    super.enter
    for (i <- 0 until 5) {addBotPlayer()}
    startRound
  }
  
  override def endRound { }

  def addBotPlayer() = {
    findNextID
    
    val addr = new InetSocketAddress("example.com", playerID)
    val tank = createTank(playerID)
    val player = new Bot(tank, "Bot " + playerID, playerID)
    tank.player = player
    players.put(addr, player)
  }
  
  override def addPlayer(addr: SocketAddress) {
    val newPlayer = !players.isDefinedAt(addr)
    
    super.addPlayer(addr)
    val player = players(addr)
    
    player.tank.destroy = false

    if (newPlayer) {
      for (projectileType <- ProjectileTypes) {
        player.gun.ammo(projectileType) = 999
      }
      player.gun.ammo(ProjectileTypes.MIRV_CLUSTER) = 0
      player.gun.ammo(ProjectileTypes.DEATHS_HEAD_CLUSTER) = 0
      player.gun.ammo(ProjectileTypes.CORBOMITE) = 0
      player.tank.corbomite = 20
      player.tank.purchasedJumpFuel = players(addr).tank.maxJumpFuel
      player.tank.jumpFuel = player.tank.purchasedJumpFuel
    }

    ground.flatten(player.tank.x)
    broadcastGround
  }
}
