package server

object Bot {
  val rng = new Random()
}

class Bot(tank: Tank, playerName: String, playerId: Byte) extends Player(tank, playerName, playerId) {

  override def timedOut = false

  override def update = {
    val rng = Bot.rng
    //Actions hacked into here because we don't have a player update yet.
    tank.gun.angle += (rng.nextFloat - 0.5f)

    if (rng.nextFloat < 0.01) tank.thrust = rng.nextInt(3) - 1
    if (rng.nextFloat < 0.7) tank.gun.firing = false
    if (rng.nextFloat < 0.004) tank.gun.firing = true
    
    if (rng.nextFloat < 0.001) tank.lift = -1
    if (rng.nextFloat < 0.04)  tank.lift = 0
    if (rng.nextFloat < 0.001) gun.cycleWeapon

    false
  }

}
