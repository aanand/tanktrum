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
    menu.showing = true

    g.translate(20, 20)

    if (null != client.me) {
      g.setColor(new Color(0f, 1f, 0f))
      g.drawString("Funds:", 20, 0)
      g.drawString(client.me.money.toString, 120, 0)
    }

    menu.render(g)

    for (player <- client.players.values) {
      if (player.ready) {
        g.setColor(new Color(0f, 1f, 0f))
      }
      else {
        g.setColor(new Color(1f, 0f, 0f))
      }
      g.drawString(player.name + ":",     300, 20 + player.id * 20)
      g.drawString(player.score.toString, 400, 20 + player.id * 20)
    }
  }

  def buy(item: Item) {
    client.sendPurchase(item.itemType.id.toByte)
  }

  def ready {
    client.sendCommand(Commands.READY)
  }
}
