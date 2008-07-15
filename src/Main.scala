import org.newdawn.slick

object Main {
  def main(args : Array[String]) {
    val game = new Game("tank")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(800, 600, false)
    container.start()
  }
}