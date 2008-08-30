import java.util.Date

object ServerMain {
  def main(args: Array[String]) {
    val server = new Server(10000)
    server.enter
    println("Server started.")
    var time = new Date().getTime()
    while (true) {
      val newTime = new Date().getTime()
      val delta = (newTime - time)
      time = newTime
      server.update(delta.toInt)
      //TODO: Should be 10-delta
      Thread.sleep(10)
    }
  }
}
