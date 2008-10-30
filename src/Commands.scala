object Commands {
  var i: Byte = 0
  def nextI() = {
    i = (i + 1).toByte
    i
  }

  val HELLO = nextI
  val GOODBYE = nextI
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
  val IMAGE_SET = nextI

  val MOVE_LEFT = nextI
  val STOP_MOVE_LEFT = nextI
  val MOVE_RIGHT = nextI
  val STOP_MOVE_RIGHT = nextI
  val JUMP = nextI
  val STOP_JUMP = nextI

  val TANK_UPDATE = nextI

  val START_FIRE = nextI
  val STOP_FIRE = nextI
  val CYCLE_WEAPON = nextI

  val READY = nextI
  val BUY = nextI
}
