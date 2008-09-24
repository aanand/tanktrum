import java.util.Date

object ServerMain {
  val TICK = 10 //ms

  def main(args: Array[String]) {
    val server = new Server(10000)

    server.start
    server !? 'enter
  }
}
