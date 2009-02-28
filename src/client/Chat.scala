package client

import shared._
import RichGraphics._

import org.newdawn.slick._

import GL._

object ChatColor extends Color(1f, 1f, 1f, 0.9f)

class Chat(client: Client) {
  val MAX_MESSAGES = Config("chat.maxMessages").toInt
  val left = Config("chat.left").toInt
  val bottom = Config("chat.bottom").toInt
  val messageHeight = Config("chat.messageHeight").toInt
  val menuOffsetX = Config("chat.menuOffsetX").toInt
  val menuOffsetY = Config("chat.menuOffsetY").toInt

  val inputField = new ChatMenuEditable("", 64)
  val inputMenu = new ChatMenu(List(("Chat: ", inputField)), menuOffsetX, menuOffsetY)
  var messages = List[(String, Player)]()
  var input = false

  def start {
    input = true
    inputField.value = ""
    inputField.perform(inputMenu)
  }

  def send = {
    input = false
    if (inputField.value.length > 0) {
      client.sendChatMessage(inputField.value) 
    }
  }

  def add(message: String, player: Player) {
    messages += (message, player)
    if (messages.length > MAX_MESSAGES) {
      messages = messages.tail
    }
  }

  def render(g: Graphics) {
    translate(left, Main.windowHeight - bottom - menuOffsetY) {
      if (input) {
        inputMenu.render(g)
      }

      translate(0, -(messageHeight * messages.length)) {
        g.setColor(ChatColor)

        for (i <- 0 until messages.length) {
          val (text, player) = messages(i)

          var x = 0
          val y = messageHeight * i

          if (null != player) {
            val playerNameString = player.name + ": "

            g.setColor(player.color)
            g.drawString(playerNameString, x, y, true)

            x += g.getFont.getWidth(playerNameString)
          }

          g.setColor(new Color(1f, 1f, 1f))
          g.drawString(text, x, y, true)
        }
      }
    }
  }

  def keyPressed(key: Int, char: Char) {
    key match {
      case Input.KEY_ENTER => send 
      case _               => inputMenu.keyPressed(key, char) }
  }
}

class ChatMenu(tree : List[(String, MenuItem)], offsetX: Int, offsetY: Int) extends Menu(tree, offsetX, offsetY) {
  override val SELECTED_COLOR = ChatColor
}

case class ChatMenuEditable(override val initValue: String, override val maxLength: Int) extends MenuEditable(initValue, maxLength) {
  override val offset = 40
}
