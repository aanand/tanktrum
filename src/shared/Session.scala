package shared
import org.newdawn.slick

import scala.collection.mutable.HashMap

trait Session {
  var active = false
  
  def byteToArray(c: Byte) = {
    val a = new Array[byte](1)
    a(0) = c.toByte
    a
  }
}
