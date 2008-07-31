import java.net._
import org.newdawn.slick._
import java.util.Date

import sbinary.Instances._
import sbinary.Operations

class Player (var tank: Tank, var name: String, var id: Byte) {
  val TIMEOUT = 10000 //in milliseconds

  var lastPing = new Date()

  override def toString = "Player: " + name

  def resetTimeout() = {
    lastPing = new Date()
  }

  def timedOut() = {
    (new Date().getTime() - lastPing.getTime()) > TIMEOUT
  }

  def serialise = {
    Operations.toByteArray((
      id,
      name
    ))
  }

  def render(g: Graphics) {
    if (null == tank || tank.isDead) {
      return
    }
    g.translate(10 + id*110, 10)
    g.setColor(tank.color)
    g.fillRect(0, 0, tank.health, 10)
    
    g.translate(0, 30)
    g.drawString(name, 0, 0)
    

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
    g.resetTransform
  }

  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[(Byte, String)](data)
    val (newID, newName) = values
    name = newName
    id = newID
  }
}
