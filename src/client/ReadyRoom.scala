package client
import org.newdawn.slick._
import shared._
import RichGraphics._
import GL._

class ReadyRoom (client: Client) {
  val itemList = Items.map(itemVal => {
      val item = Items.items(itemVal)
      (item.name, MenuCommandWithLabel(Unit => buy(item), "£" + item.cost.toString))
  }).toList

  val menu = new Menu(
    itemList ++
    List(("Ready", MenuCommand(Unit => ready))), 40, 40)

  def render(g: Graphics) {
    g.setColor(new Color(0f, 0f, 0f, 0.5f))
    g.fillRect(0, 0, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT)
    
    menu.showing = true

    if (null != client.me) {
      translate(20, 20) {

        g.setColor(new Color(0f, 1f, 0f))
        g.drawString("Funds:", 20, 0, true)
        g.drawString("£" + client.me.money.toString, 120, 0, true)

        g.drawString("#", 180, 0, true)
        for (item <- Items) {
          if (client.me.items.isDefinedAt(item)) {
            val num = client.me.items(item)
            g.drawString(num.toString, 180, 20+20*item.id, true)
          }
        }
      }
    }

    menu.render(g)
    
    var offset = 0

    translate(20, 20) {
      g.setColor(new Color(0f, 1f, 0f))
      g.drawString("Players", 450, 0, true)
      translate(0, 20) {
        for (player <- client.players.values.toList.sort((p1, p2) => {p1.score > p2.score})) {
          val col = (player.color)
          if (player.ready) {
            g.setColor(new Color(col.getRed, col.getGreen, col.getBlue, 1f))
            g.drawString(player.name + " is go.", 350, offset * 20, true)
          }
          else {
            g.setColor(new Color(col.getRed, col.getGreen, col.getBlue, 0.5f))
            g.drawString(player.name, 350, offset * 20, true)
          }
          g.drawString(player.score.toString, 600, offset * 20, true)
          offset += 1
        }
      }
    }
  }
  
  def mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    menu.mouseMoved(oldx, oldy, newx, newy)
  }
  
  def mouseClicked(button: Int, x: Int, y: Int, clickCount: Int) {
    menu.mouseClicked(button, x, y, clickCount)
  }

  def buy(item: Item) {
    client.sendPurchase(item.itemType.id.toByte)
  }

  def ready {
    client.sendCommand(Commands.READY)
  }
}
