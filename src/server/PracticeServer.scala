package server

import shared._

import java.nio.channels._
import java.nio._
import java.net._

class PracticeServer(port : Int) extends Server(port) {
  override def enter() {
    public = false
    super.enter
    startRound
  }
  
  override def endRound { }
  
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
  }
}
