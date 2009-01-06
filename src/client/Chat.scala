package client

import shared._

import org.newdawn.slick._
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object ChatColor extends Color(0f, 0f, 1f)

object Chat {
  val height = 100
  val width = 325
}

class ChatBox(session: Session) extends GameObject(session) {
  override def shapes = {
    println("Creating chat box.")
    val box = new PolygonDef()
    box.setAsBox(Chat.width/(2*Main.GAME_WINDOW_RATIO), 
                 Chat.height/(2*Main.GAME_WINDOW_RATIO), 
                 new Vec2(Chat.width/(2*Main.GAME_WINDOW_RATIO), 
                         (Main.WINDOW_HEIGHT-(Chat.height/2))/Main.GAME_WINDOW_RATIO), 
                 0)
    List(box)
  }
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
    g.fillRect(0, Main.WINDOW_HEIGHT - Chat.height, Chat.width, Main.WINDOW_HEIGHT)
    if (input) {
      g.resetTransform
      g.translate(0, 560)
      inputMenu.render(g)
    }
    g.resetTransform
    g.translate(20, 560 - 15*messages.length)
    g.setColor(ChatColor)
    for (message <- messages) {
      g.translate(0, 15)
      g.drawString(message, 0, 0)
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
