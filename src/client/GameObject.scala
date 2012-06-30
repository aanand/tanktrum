package client

import java.lang.Math._

abstract class GameObject() {
  var x: Float = -1f
  var y: Float = -1f
  var angle: Float = _

  def dist(x2: Float, y2: Float) = {
    sqrt(pow(x-x2, 2) + pow(y-y2, 2))
  }
}
