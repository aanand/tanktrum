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

  def toMenu() = {
    new Submenu(toList ++ List(
      ("Cancel", new CancelCommandsMenuItem),
      ("Save", new SaveKeyCommandsMenuItem)))
  }

  def save() = for (command <- this) command._2.save
  def cancel() = for (command <- this) command._2.cancel
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
      }
    }
  }

  def save() = Prefs.save("key." + configName, key.toString)
  def cancel() = {
    key = Prefs("key." + configName).toInt
    value = Input.getKeyName(key)
  }
}

class SaveKeyCommandsMenuItem extends MenuItem {
  override def perform(menu : Menu) = {
    KeyCommands.save
    menu.popPath
  }
}

class CancelCommandsMenuItem extends MenuItem {
  override def perform(menu : Menu) = {
    KeyCommands.cancel
    menu.popPath
  }
}
