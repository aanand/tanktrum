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
  val EXPLOSION = nextI
  val PLAYERS = nextI
  val SERVER_FULL = nextI
  val READY_ROOM = nextI
  val CHAT_MESSAGE = nextI

  val MOVE_LEFT = nextI
  val STOP_MOVE_LEFT = nextI
  val MOVE_RIGHT = nextI
  val STOP_MOVE_RIGHT = nextI
  val JUMP = nextI
  val STOP_JUMP = nextI

  val AIM_CLOCKWISE = nextI
  val STOP_AIM_CLOCKWISE = nextI
  val AIM_ANTICLOCKWISE = nextI
  val STOP_AIM_ANTICLOCKWISE = nextI

  val POWER_UP = nextI
  val STOP_POWER_UP = nextI
  val POWER_DOWN = nextI
  val STOP_POWER_DOWN = nextI

  val START_FIRE = nextI
  val STOP_FIRE = nextI
  val CYCLE_WEAPON = nextI

  val READY = nextI
  val BUY = nextI
}
