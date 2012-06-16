package shared

import org.newdawn.slick

object Main {
  val game = new Game("Boom Trapezoid")
  var container: slick.AppGameContainer = _
  
  val INITIAL_WINDOW_WIDTH  = Prefs("window.width").toInt
  val INITIAL_WINDOW_HEIGHT = Prefs("window.height").toInt
  val INITIAL_FULLSCREEN = Prefs("window.fullscreen").toBoolean
  
  val GAME_WIDTH = Config("game.width").toFloat
  val GAME_HEIGHT = Config("game.height").toFloat
  val GAME_SCALE = Config("game.scale").toFloat

  def windowWidth = container.getWidth
  def windowHeight = container.getHeight

  val VERSION = 2

  val logicUpdateInterval = Config("game.logicUpdateInterval").toInt
  val targetFrameRate = Config("game.targetFrameRate").toInt
  val smoothDeltas = Config("game.smoothDeltas").toBoolean
  val showFPS = Config("game.showFPS").toBoolean

  def main(args : Array[String]) {
    container = new slick.AppGameContainer(game)
    container.setDisplayMode(INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT, INITIAL_FULLSCREEN)

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
