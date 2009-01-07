package shared

import org.newdawn.slick

object Main {
  val WINDOW_WIDTH  = Config("window.width").toInt
  val WINDOW_HEIGHT = Config("window.height").toInt
  
  val GAME_WIDTH = Config("game.width").toFloat
  val GAME_HEIGHT = Config("game.height").toFloat

  val GAME_WINDOW_RATIO = WINDOW_WIDTH/GAME_WIDTH

  val VERSION = 1

  val logicUpdateInterval = Config("game.logicUpdateInterval").toInt
  val targetFrameRate = Config("game.targetFrameRate").toInt
  val smoothDeltas = Config("game.smoothDeltas").toBoolean
  val showFPS = Config("game.showFPS").toBoolean

  def main(args : Array[String]) {
    val game = new Game("Boom Trapezoid")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(WINDOW_WIDTH, WINDOW_HEIGHT, false)

    if (logicUpdateInterval > 0) {
      container.setMaximumLogicUpdateInterval(logicUpdateInterval)
      container.setMinimumLogicUpdateInterval(logicUpdateInterval)
    }
    
    if (targetFrameRate > 0) {
      container.setTargetFrameRate(targetFrameRate)
    }
    
    container.setShowFPS(showFPS)

    //it's useful if the server still sends updates when the player is alt-tabbed
    container.setUpdateOnlyWhenVisible(false)
    container.start()
  }
}
