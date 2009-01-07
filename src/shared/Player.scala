package shared
object Player {
  val MAX_NAME_LENGTH = Config("player.maxNameLength").toInt
}

class Player {
  var name: String = "Unknown"
  var id: Byte = -1

  var score = 0
  var money = Config("player.startingMoney").toInt

  var me = false

  var updated = true
  var ready = false


  if (null != name && name.length > Player.MAX_NAME_LENGTH) {
    name = name.substring(0, Player.MAX_NAME_LENGTH)
  }

  override def toString = "Player: " + name
}

