import org.newdawn.slick._

class ReadyRoom (client: Client) {
  val itemList = Items.map(itemVal => {
      val item = Items.items(itemVal)
      (item.name + ": " + item.cost, MenuCommand(Unit => buy(item)))
  }).toList

  val menu = new Menu(
    itemList ++
    List(("Ready", MenuCommand(Unit => ready))))

  def render(g: Graphics) {
    menu.showing = true
    if (null != client.me) {
      g.setColor(new Color(0f, 1f, 0f))
      g.drawString("Funds: " + client.me.money, 20, 0)
    }
    g.translate(20, 0)
    menu.render(g)

    for (player <- client.players.values) {
      if (player.ready) {
        g.setColor(new Color(0f, 1f, 0f))
      }
      else {
        g.setColor(new Color(1f, 0f, 0f))
      }
      g.drawString(player.name + ": " + player.score, 400, player.id * 20)
    }
  }

  def buy(item: Item) {
    client.sendPurchase(item.itemType.id.toByte)
  }

  def ready {
    client.sendCommand(Commands.READY)
  }
}
