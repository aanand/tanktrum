package client
import shared._
import scala.collection.mutable.LinkedList

class ServerList(game: Game) extends Menu(List(("Filter", new MenuEditable("", 32)))) {
  hide

  def show(userName: String) {
    super.show

    var list = List[(String, MenuItem)](("Filter", new MenuEditable("", 32)))

    list = list + ("boomtrapezoid.com:10000", new MenuCommand(Unit => connect("boomtrapezoid.com", 10000, userName)))
    list = list + ("localhost:10000", new MenuCommand(Unit => connect("localhost", 10000, userName)))
    tree = buildTree(list)
  }

  def connect(address: String, port: Int, userName: String) {
    hide
    game.startClient(address, port, userName)
  }
}
