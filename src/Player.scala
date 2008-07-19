import java.net._
import java.util.Date

class Player (tank: Tank, name: String) {
  val TIMEOUT = 10000 //in milliseconds

  var lastPing = new Date()

  override def toString = "Player: " + name

  def resetTimeout() = {
    lastPing = new Date()
  }

  def timedOut() = {
    (new Date().getTime() - lastPing.getTime()) > TIMEOUT
  }

  def getName = name
}
