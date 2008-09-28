import org.newdawn.slick

object Main {
  val WIDTH  = Config("game.width").toInt
  val HEIGHT = Config("game.height").toInt

  val logicUpdateInterval = Config("game.logicUpdateInterval").toInt

  def main(args : Array[String]) {
    val game = new Game("Boom Trapezoid")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(WIDTH, HEIGHT, false)
    container.setShowFPS(true)
    container.setMaximumLogicUpdateInterval(logicUpdateInterval)
    container.setMinimumLogicUpdateInterval(logicUpdateInterval)
    
    //it's useful if the server still sends updates when the player is alt-tabbed
    container.setUpdateOnlyWhenVisible(false)
    container.start()
  }
}
