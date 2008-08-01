import org.newdawn.slick._

class Shop (client: Client) {
  val menu = new Menu(List(
    (Nuke.name + ": " + Nuke.cost, MenuCommand(Unit => buyNuke)),
    (Roller.name + ": " + Roller.cost, MenuCommand(Unit => buyRoller))))
  menu.showing = false

  def render(g: Graphics) {
    if (null != client.me) {
      g.drawString("Funds: " + client.me.money, 0, 0)
      g.translate(0, 20)
    }
    menu.render(g)
  }

  def buyNuke {
    println("Buying a nuke.")
    client.sendCommand(Commands.BUY_NUKE)
  }

  def buyRoller {
    println("Buying a rollermine.")
    client.sendCommand(Commands.BUY_ROLLER)
  }
}
