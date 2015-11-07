package edu.ufl.dos15.fbapi

import scala.util.Try
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http

object Main {
    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val serverHost = Try(config.getString("server.host")).getOrElse("localhost")
        val serverPort = Try(config.getInt("server.port")).getOrElse(8080)

        implicit val system = ActorSystem("FacebookSystem")
        val server= system.actorOf(Props[Server], "server")
        IO(Http) ! Http.Bind(server, serverHost, serverPort)
    }
}