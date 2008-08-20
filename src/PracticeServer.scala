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
    
    if (newPlayer) {
      sendGround(addr)
    }
  }
}