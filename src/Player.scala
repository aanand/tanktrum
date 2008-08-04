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
  var money = 1000

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

    if (tank.isAlive) {
      g.translate(10, 30)
      
      tank.selectedWeapon match {
        case ProjectileTypes.PROJECTILE => {
          g.fillOval(-3, -3, 6, 6)
        }
        case ProjectileTypes.NUKE => {
          g.fillOval(-6, -6, 12, 12)
        }
        case ProjectileTypes.ROLLER => {
          g.setColor(new Color(0f, 0f, 1f))
          g.fillOval(-6, -6, 12, 12)
        }
      }
      g.drawString(tank.ammo(tank.selectedWeapon).toString, 15, -10)
    }
    g.resetTransform

    tank.render(g)
  }

  def buyNuke = {
    if (money >= Nuke.cost) { 
      money -= Nuke.cost
      tank.ammo(ProjectileTypes.NUKE) = tank.ammo(ProjectileTypes.NUKE) + Nuke.ammo
    }
  }
  
  def buyRoller = {
    if (money >= Roller.cost) { 
      money -= Roller.cost
      tank.ammo(ProjectileTypes.ROLLER) = tank.ammo(ProjectileTypes.ROLLER) + Roller.ammo
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
