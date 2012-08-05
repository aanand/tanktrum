package server

class Bot(tank: Tank, playerName: String, playerId: Byte) extends Player(tank, playerName, playerId) {
  override def timedOut = false
}
