package client

import shared._
import RichGraphics._
import GL._

import scala.collection.mutable.HashMap

import org.newdawn.slick._
import java.net._
import sbinary.Instances._
import sbinary.Operations

class Player extends shared.Player {

  var tank: Tank = _
  def gun = tank.gun
  def color = Colors(id)
  val items = new HashMap[Items.Value, Int]

  var flashScore = 0f

  def render(g: Graphics, spriteColor: Color) {
    if (flashScore > 0.01f) flashScore -= 0.01f
    if (flashScore < -0.01f) flashScore += 0.01f
    if (null == tank) {
      return
    }

    translate(10 + id*110, 10) {
      val scoreColor = new Color(flashScore + color.r, flashScore + color.g, flashScore + color.b, color.a)
      g.setColor(scoreColor)

      tank.render(g, 10, 28, 0, 4, false)
      g.drawString(score.toString, 28, 16, true);
      
      g.setColor(color)
      g.drawString(name, 0, 0, true)

      translate(0, 32) {
        g.fillRect(0, 0, tank.health, 10)
    
        translate(0, 12) {
          g.fillRect(0, 0, tank.fuelPercent, 5)

          if (tank.isAlive) {
            translate(10, 20) {
              Projectile.render(g, gun.selectedWeapon, spriteColor)
              g.drawString(gun.ammo(gun.selectedWeapon).toString, 15, -9, true)
            }
          }
        }
      }
    }
  }

  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[(Byte, Boolean, Boolean, Int, Int, String)](data)
    val (newID, newMe, newReady, newScore, newMoney, newName) = values

    //TODO: These should maybe be Config options?
    if (newName.length > shared.Player.MAX_NAME_LENGTH) {
      name = newName.substring(0, shared.Player.MAX_NAME_LENGTH)
    }
    else {
      name = newName
    }
    me = newMe
    ready = newReady
    if (score != newScore) flashScore += (newScore-score)/5
    score = newScore
    money = newMoney
    id = newID
  }
}

object Colors {
  def apply(i: Int) = colors(cycle(i))
  def cycle(i: Int) = i%colors.length

  val colors = Array(
    new Color(1f, 0f, 0f),
    new Color(0f, 1f, 0f),
    new Color(0f, 0f, 1f),
    new Color(1f, 1f, 0f),
    new Color(1f, 0f, 1f),
    new Color(0f, 1f, 1f))
}
