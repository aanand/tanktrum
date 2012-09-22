package client

import shared._
import RichGraphics._
import GL._

import scala.collection.mutable.HashMap

import org.newdawn.slick._
import java.net._
import sbinary.Instances._
import sbinary.Operations

class Player(client: Client) extends shared.Player {

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

    val position: Int = 
      if (id > client.me.id) id-1 
      else                   id

    val translateX = 
      if (equals(client.me)) 10  
      else                   Main.windowWidth - 120 - position*110

    translate(translateX, 10) {
      val scoreColor = new Color(flashScore + color.r, flashScore + color.g, flashScore + color.b, color.a)
      g.setColor(scoreColor)

      if (equals(client.me)) {
        tank.render(g, 20, 28, 0, 8, false)
        g.drawString(score.toString, 42, 16, true);
        g.drawString(name, 42, 0, true)
      } else {
        tank.render(g, 10, 28, 0, 4, false)
        g.drawString(score.toString, 28, 16, true);
        g.drawString(name, 0, 0, true)
      }

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
    new Color(230,  230,  230),
    new Color(236, 65,  0),
    new Color(229, 205, 54),
    new Color(0,   140, 170),
    new Color(70,  97,  26),
    new Color(92,  48,  110))
}
