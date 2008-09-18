import org.newdawn.slick

object Main {
  val WIDTH  = Config("game.width").toInt
  val HEIGHT = Config("game.height").toInt

  val logicUpdateInterval = Config("game.logicUpdateInterval").toInt
  val targetFrameRate = Config("game.targetFrameRate").toInt
  val smoothDeltas = Config("game.smoothDeltas").toBoolean
  val showFPS = Config("game.showFPS").toBoolean

  def main(args : Array[String]) {
    val game = new Game("Boom Trapezoid")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(WIDTH, HEIGHT, false)

    if (logicUpdateInterval > 0) {
      container.setMaximumLogicUpdateInterval(logicUpdateInterval)
      container.setMinimumLogicUpdateInterval(logicUpdateInterval)
    }
    
    if (targetFrameRate > 0) {
      container.setTargetFrameRate(targetFrameRate)
    }
    
    container.setSmoothDeltas(smoothDeltas)

    container.setShowFPS(showFPS)

    //it's useful if the server still sends updates when the player is alt-tabbed
    container.setUpdateOnlyWhenVisible(false)
    container.start()
  }
}
