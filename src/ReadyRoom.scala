import org.newdawn.slick._

class ReadyRoom (client: Client) {
  val itemList = Items.map(itemVal => {
      val item = Items.items(itemVal)
      (item.name, MenuCommandWithLabel(Unit => buy(item), item.cost.toString))
  }).toList

  val menu = new Menu(
    itemList ++
    List(("Ready", MenuCommand(Unit => ready))))

  def render(g: Graphics) {
    g.resetTransform

    menu.showing = true

    g.translate(20, 20)

    if (null != client.me) {
      g.setColor(new Color(0f, 1f, 0f))
      g.drawString("Funds:", 20, 0)
      g.drawString(client.me.money.toString, 120, 0)
    }

    menu.render(g)
    
    g.resetTransform

    var offset = 0
    for (player <- client.players.values.toList.sort((p1, p2) => {p1.score > p2.score})) {
      val col = (player.color)
      if (player.ready) {
        g.setColor(new Color(col.getRed, col.getGreen, col.getBlue, 1f))
        g.drawString(player.name + " is go.", 300, 20 + offset * 20)
      }
      else {
        g.setColor(new Color(col.getRed, col.getGreen, col.getBlue, 0.5f))
        g.drawString(player.name, 300, 20 + offset * 20)
      }
      g.drawString(player.score.toString, 400, 20 + offset * 20)
      offset += 1
    }
    g.resetTransform
    g.scale(Main.GAME_WINDOW_RATIO, Main.GAME_WINDOW_RATIO)
  }

  def buy(item: Item) {
    client.sendPurchase(item.itemType.id.toByte)
  }

  def ready {
    client.sendCommand(Commands.READY)
  }
}
