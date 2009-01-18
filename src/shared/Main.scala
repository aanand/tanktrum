package shared

import org.newdawn.slick

object Main {
  val game = new Game("Boom Trapezoid")
  val container = new slick.AppGameContainer(game)
  
  val INITIAL_WINDOW_WIDTH  = Prefs("windowWidth", "window.width").toInt
  val INITIAL_WINDOW_HEIGHT = Prefs("windowHeight", "window.height").toInt
  
  val GAME_WIDTH = Config("game.width").toFloat
  val GAME_HEIGHT = Config("game.height").toFloat

  def windowWidth = container.getWidth
  def windowHeight = container.getHeight
  def gameWindowWidthRatio = windowWidth/GAME_WIDTH
  def gameWindowHeightRatio = windowHeight/GAME_HEIGHT

  val VERSION = 2

  val logicUpdateInterval = Config("game.logicUpdateInterval").toInt
  val targetFrameRate = Config("game.targetFrameRate").toInt
  val smoothDeltas = Config("game.smoothDeltas").toBoolean
  val showFPS = Config("game.showFPS").toBoolean

  def main(args : Array[String]) {
    container.setDisplayMode(INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT, false)

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
