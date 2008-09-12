import org.newdawn.slick

object Main {
  val WIDTH = 800
  val HEIGHT = 600

  def main(args : Array[String]) {
    val game = new Game("Boom Trapezoid")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(WIDTH, HEIGHT, false)
    container.setShowFPS(false)
    container.setMaximumLogicUpdateInterval(1000/100)
    container.setMinimumLogicUpdateInterval(1000/100)
    
    //it's useful if the server still sends updates when the player is alt-tabbed
    container.setUpdateOnlyWhenVisible(false)
    container.start()
  }
}
