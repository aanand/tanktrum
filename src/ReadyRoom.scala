import org.newdawn.slick._

class ReadyRoom (client: Client) {
  val menu = new Menu(List(
    (Nuke.name + ": " + Nuke.cost, MenuCommand(Unit => buyNuke)),
    (Roller.name + ": " + Roller.cost, MenuCommand(Unit => buyRoller)),
    ("Ready", MenuCommand(Unit => ready))))

  def render(g: Graphics) {
    menu.showing = true
    if (null != client.me) {
      g.drawString("Funds: " + client.me.money, 0, 0)
      g.translate(0, 20)
    }
    menu.render(g)

    for (player <- client.players.values) {
      if (player.ready) {
        g.setColor(new Color(0f, 1f, 0f))
      }
      else {
        g.setColor(new Color(1f, 0f, 0f))
      }
      g.drawString(player.name + ": " + player.score, 700, player.id * 20)
    }
  }

  def buyNuke {
    println("Buying a nuke.")
    client.sendCommand(Commands.BUY_NUKE)
  }

  def buyRoller {
    println("Buying a rollermine.")
    client.sendCommand(Commands.BUY_ROLLER)
  }

  def ready {
    client.sendCommand(Commands.READY)
  }
}
