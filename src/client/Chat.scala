package client

import shared._
import RichGraphics._

import org.newdawn.slick._

import GL._

object ChatColor extends Color(1f, 1f, 1f, 0.9f)

object Chat {
  val height = 100
  val width = 325
}

class Chat(client: Client) {
  val MAX_MESSAGES = Config("chat.maxMessages").toInt

  val inputField = new ChatMenuEditable("", 64)
  val inputMenu = new ChatMenu(List(("Chat: ", inputField)))
  var messages = List[String]()
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

  def add(message: String) {
    messages += message
    if (messages.length > MAX_MESSAGES) {
      messages = messages.tail
    }
  }

  def render(g: Graphics) {
    g.setColor(new Color(0f, 0f, 0f, 0.7f))
    //g.fillRect(0, Main.WINDOW_HEIGHT - Chat.height, Chat.width, Main.WINDOW_HEIGHT)
    translate(0, 560) {
      if (input) {
        inputMenu.render(g)
      }
      translate(20, -15*messages.length) {
        g.setColor(ChatColor)
      }

      var i = 1
      for (message <- messages) {
        translate(0, 15 * i) {
          i+=1
          g.drawString(message, 0, 0, true)
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

class ChatMenu(tree : List[(String, MenuItem)]) extends Menu(tree) {
  override val SELECTED_COLOR = ChatColor
}

case class ChatMenuEditable(override val initValue: String, override val maxLength: Int) extends MenuEditable(initValue, maxLength) {
  override val offset = 40
}
