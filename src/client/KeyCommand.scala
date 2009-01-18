package client

import shared._
import org.newdawn.slick.Input
import scala.collection.mutable._

object KeyCommands extends ArrayBuffer[(String, KeyCommand)] {
  val chat = new KeyCommand("Chat", "chat")
  val left = new KeyCommand("Left", "left")
  val right = new KeyCommand("Right", "right")
  val jump = new KeyCommand("Jump", "jump")
  val aimClockwise = new KeyCommand("Aim Clockwise", "aimClockwise")
  val aimAnticlockwise = new KeyCommand("Aim Anticlockwise", "aimAnticlockwise")
  val powerUp = new KeyCommand("Power Up", "powerUp")
  val powerDown = new KeyCommand("Power Down", "powerDown")
  val fire = new KeyCommand("Fire", "fire")
  val cycleWeapon = new KeyCommand("Change Weapon", "cycleWeapon")
}

class KeyCommand(name: String, configName: String) extends MenuEditable("", 255) {
  KeyCommands.append((name, this))

  var key = Prefs("key." + configName).toInt

  override val offset = 150
  value = Input.getKeyName(key)

  override def keyPressed(key : Int, char : Char, menu : Menu) {
    key match {
      case Input.KEY_ESCAPE => menu.editing = false 
      case _ => {
        this.key = key
        value = Input.getKeyName(key)
        menu.editing = false
        Prefs.save("key." + configName, key.toString)
      }
    }
  }
}


