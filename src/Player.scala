import java.net._
import org.newdawn.slick._
import java.util.Date

import sbinary.Instances._
import sbinary.Operations

object Player {
  val MAX_NAME_LENGTH = 16
}

class Player (var tank: Tank, var name: String, var id: Byte) {
  val TIMEOUT = 10000 //in milliseconds
  
  var lastPing = new Date()

  var score = 0
  var money = 0

  var me = false

  var updated = true
  var ready = false

  if (null != name && name.length > Player.MAX_NAME_LENGTH) {
    name = name.substring(0, Player.MAX_NAME_LENGTH)
  }

  override def toString = "Player: " + name

  def resetTimeout() = {
    lastPing = new Date()
  }

  def timedOut() = {
    (new Date().getTime() - lastPing.getTime()) > TIMEOUT
  }

  def render(g: Graphics) {
    if (null == tank) {
      return
    }
    g.translate(10 + id*110, 10)
    g.setColor(tank.color)
    g.fillRect(0, 0, tank.health, 10)
    
    g.translate(0, 10)
    g.drawString(name + ": " + score, 0, 0)

    g.translate(0, 20)
    g.fillRect(0, 0, tank.fuelPercent, 5)

    if (tank.isAlive) {
      g.translate(10, 30)

      ProjectileTypes.render(g, tank.selectedWeapon)

      g.drawString(tank.ammo(tank.selectedWeapon).toString, 15, -10)
    }
    g.resetTransform

    tank.render(g)
  }

  def buy(item: Item) = {
    if (money >= item.cost) { 
      money -= item.cost
      if (item.projectileType != null) {
        tank.ammo(item.projectileType) = tank.ammo(item.projectileType) + item.units
      }
      if (item == JumpjetItem) {
        tank.purchasedJumpFuel += item.units
        if (tank.purchasedJumpFuel > tank.maxJumpFuel) {
          tank.purchasedJumpFuel = tank.maxJumpFuel
        }
      }
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
