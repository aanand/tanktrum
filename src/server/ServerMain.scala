package server

import shared._
import java.util.Date

object ServerMain {
  val TICK = 10 //ms

  def main(args: Array[String]) {
    val server = new Server(Config("default.port").toInt, 
                            Config("server.name"), 
                            Config("server.public").toBoolean)

    server.start
    server !? 'enter
  }
}
