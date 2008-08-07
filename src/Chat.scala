import org.newdawn.slick._

object ChatColor extends Color(0f, 0f, 1f)

class Chat(client: Client) {
  val MAX_MESSAGES = 5
  val inputField = new ChatMenuEditable("", 512)
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
      case Input.KEY_ENTER => { send }
      case _ => { inputMenu.keyPressed(key, char) }
    }
  }
}

class ChatMenu(tree : List[(String, MenuItem)]) extends Menu(tree) {
  override val SELECTED_COLOR = ChatColor
}

case class ChatMenuEditable(override val initValue: String, override val maxLength: Int) extends MenuEditable(initValue, maxLength) {
  override val offset = 40
}

