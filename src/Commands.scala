object Commands {
  var i: Byte = 0
  def nextI() = {
    i = (i + 1).toByte
    i
  }

  val HELLO = nextI
  val PING  = nextI
  val GROUND = nextI
  val TANKS = nextI
  val PROJECTILE = nextI
  val PROJECTILES = nextI

  val MOVE_LEFT = nextI
  val STOP_MOVE_LEFT = nextI
  val MOVE_RIGHT = nextI
  val STOP_MOVE_RIGHT = nextI

  val AIM_CLOCKWISE = nextI
  val STOP_AIM_CLOCKWISE = nextI
  val AIM_ANTICLOCKWISE = nextI
  val STOP_AIM_ANTICLOCKWISE = nextI

  val POWER_UP = nextI
  val STOP_POWER_UP = nextI
  val POWER_DOWN = nextI
  val STOP_POWER_DOWN = nextI

  val FIRE = nextI
  val CYCLE_WEAPON = nextI
}
