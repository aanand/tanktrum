import org.newdawn.slick

object Main {
  def main(args : Array[String]) {
    val game = new Game("tank")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(800, 600, false)
    container.setShowFPS(false)
    container.setMaximumLogicUpdateInterval(1000/25)
    
    //it's useful if the server still sends updates when the player is alt-tabbed
    container.setUpdateOnlyWhenVisible(false)
    container.start()
  }
}
