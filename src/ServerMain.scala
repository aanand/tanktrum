import java.util.Date

object ServerMain {
  val TICK = 10 //ms

  def main(args: Array[String]) {
    val server = new Server(Config("default.port").toInt)

    server.start
    server !? 'enter
  }
}
