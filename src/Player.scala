import java.net._
import org.newdawn.slick._
import java.util.Date

import sbinary.Instances._
import sbinary.Operations

object Player {
  val MAX_NAME_LENGTH = Config("player.maxNameLength").toInt
}

class Player (var tank: Tank, var name: String, var id: Byte) {
  val TIMEOUT = Config("player.timeout").toInt
  
  var lastPing = new Date()

  var score = 0
  var money = 0

  var me = false

  var updated = true
  var ready = false

  def gun = tank.gun

  if (null != name && name.length > Player.MAX_NAME_LENGTH) {
    name = name.substring(0, Player.MAX_NAME_LENGTH)
  }

  override def toString = "Player: " + name
  
  def color = Colors(id)

  def resetTimeout() = {
    lastPing = new Date()
  }

  def timedOut() = {
    (new Date().getTime() - lastPing.getTime()) > TIMEOUT
  }

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

  def buy(item: Item) = {
    if (money >= item.cost) { 
      money -= item.cost
      if (item.projectileType != null) {
        gun.ammo(item.projectileType) = gun.ammo(item.projectileType) + item.units
      }
      if (item == JumpjetItem) {
        tank.purchasedJumpFuel += item.units
        tank.jumpFuel = tank.purchasedJumpFuel
        if (tank.purchasedJumpFuel > tank.maxJumpFuel) {
          tank.purchasedJumpFuel = tank.maxJumpFuel
        }
      }
      if (item == CorbomiteItem) {
        tank.corbomite += item.units
        if (tank.corbomite > tank.maxCorbomite) {
          tank.corbomite = tank.maxCorbomite
        }
      }
    }
  }

  def awardHit(tank: Tank, damage: Int) {
    var pointsAwarded = 0
    if (tank == this.tank) {
      pointsAwarded = -damage
    }
    else {
      pointsAwarded = damage
      
      //Ugly ugly ugly. :(
      if (tank.session.asInstanceOf[Server].leader.tank == tank) {
        pointsAwarded = (1.5 * pointsAwarded).toInt
        println("Extra points for hitting the leader.")
      }
    }

    //Don't allow negative score or money.
    if (score + pointsAwarded > 0) {
      score += pointsAwarded
    }
    if (money + pointsAwarded > 0) {
      money  += pointsAwarded
    }
  }
  
  def serialise = {
    Operations.toByteArray((
      id,
      me,
      ready,
      score,
      money,
      name
    ))
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
