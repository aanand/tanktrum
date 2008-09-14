import org.newdawn.slick

object Main {
  val WIDTH = 800
  val HEIGHT = 600

  def main(args : Array[String]) {
    val game = new Game("Boom Trapezoid")
    
    val container = new slick.AppGameContainer(game)
    container.setDisplayMode(WIDTH, HEIGHT, false)
    container.setTargetFrameRate(100)
    container.setShowFPS(false)
    container.setUpdateOnlyWhenVisible(false)
    container.start()
  }
}
