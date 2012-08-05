package server

import shared._

import java.nio.channels._
import java.nio._
import java.net._

class PracticeServer(port : Int) extends Server(port, "", false) {
  override def enter() {
    super.enter
  }
  
  override def endRound { }

  def addBotPlayer() = {
    findNextID
    
    val addr = new InetSocketAddress("localhost", playerID + 1024)
    val tank = createTank(playerID)
    val player = new Bot(tank, "Bot " + playerID, playerID)
    tank.player = player
    players.put(addr, player)

    giveAmmo(player)
  }

  override def addPlayer(addr: SocketAddress) {
    val newPlayer = !players.isDefinedAt(addr)
    
    super.addPlayer(addr)
    val player = players(addr)
    
    player.tank.destroy = false

    if (newPlayer) giveAmmo(player)

    for (i <- 0 until 5) {addBotPlayer()}
    startRound
  }

  def giveAmmo(player: Player) {
    player.gun.ammo(ProjectileTypes.NUKE) = 1
    player.gun.ammo(ProjectileTypes.ROLLER) = 3
    player.gun.ammo(ProjectileTypes.MIRV) = 1
    player.gun.ammo(ProjectileTypes.MACHINE_GUN) = 20
    player.gun.ammo(ProjectileTypes.DEATHS_HEAD) = 1
    player.gun.ammo(ProjectileTypes.MISSILE) = 3
    
    player.tank.corbomite = 10
    player.tank.purchasedJumpFuel = player.tank.maxJumpFuel/2
    player.tank.jumpFuel = player.tank.purchasedJumpFuel
  }
}
