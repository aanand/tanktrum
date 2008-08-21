import org.newdawn.slick
import org.newdawn.slick._

import java.nio.channels._
import java.nio._
import java.net._

class PracticeServer(port : Int) extends Server(port) {
  override def enter {
    super.enter
    startRound
  }
  
  override def endRound { }
  
  override def addPlayer(addr: SocketAddress) {
    val newPlayer = !players.isDefinedAt(addr)
    
    super.addPlayer(addr)
    players(addr).tank.health = 100

    if (newPlayer) {
      for (projectileType <- ProjectileTypes) {
        players(addr).tank.ammo(projectileType) = 999
      }
    }
  }
}
