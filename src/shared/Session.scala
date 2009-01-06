package shared
import org.newdawn.slick

import scala.collection.mutable.HashMap

abstract class Session(container: slick.GameContainer) {

  var active = false

  var supposedRunTime = 0
  var numTankUpdates = 0
  var startTime: Long = 0

  def enter() {
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def update(delta: Int) {
    supposedRunTime += delta
  }
  
  
  def byteToArray(c: Byte) = {
    val a = new Array[byte](1)
    a(0) = c.toByte
    a
  }

  def isActive = active
}
