import org.newdawn.slick._
import java.net._
import sbinary.Instances._
import sbinary.Operations

class ClientPlayer extends Player {

  var tank: ClientTank = _
  def gun = tank.gun
  def color = Colors(id)

  def render(g: Graphics) {
    g.resetTransform
    if (null == tank) {
      return
    }

    g.translate(10 + id*110, 10)
    g.setColor(color)

    g.drawString(name, 0, 0)

    g.translate(0, 16)
    g.fillRect(0, 0, tank.health, 10)
    
    g.translate(0, 12)
    g.fillRect(0, 0, tank.fuelPercent, 5)

    if (tank.isAlive) {
      g.translate(10, 20)

      ProjectileTypes.render(g, gun.selectedWeapon)

      g.drawString(gun.ammo(gun.selectedWeapon).toString, 15, -9)
    }

    g.resetTransform
    g.scale(Main.GAME_WINDOW_RATIO, Main.GAME_WINDOW_RATIO)
  }

  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[(Byte, Boolean, Boolean, Int, Int, String)](data)
    val (newID, newMe, newReady, newScore, newMoney, newName) = values
    if (newName.length > Player.MAX_NAME_LENGTH) {
      name = newName.substring(0, Player.MAX_NAME_LENGTH)
    }
    else {
      name = newName
    }
    me = newMe
    ready = newReady
    score = newScore
    money = newMoney
    id = newID
  }
}

object Colors {
  def apply(i: Int) = colors(i%colors.length)

  val colors = Array(
    new Color(1f, 0f, 0f),
    new Color(0f, 1f, 0f),
    new Color(0f, 0f, 1f),
    new Color(1f, 1f, 0f),
    new Color(1f, 0f, 1f),
    new Color(0f, 1f, 1f))
}