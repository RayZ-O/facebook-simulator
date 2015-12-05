package edu.ufl.dos15.fbapi

import akka.actor.{Actor, ActorLogging, Props}
import spray.routing.RequestContext
import com.roundeights.hasher.Implicits._
import spray.http.StatusCodes
import spray.routing.HttpService

class AuthActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/authdb")
  val ctx = reqctx

  message match {
    case Register(name, passwd, pub) =>
      context.become(timeoutBehaviour orElse waitingRegister)
      sendToDB(Register(name, repeatedHash(3, passwd), pub))

    case pwa: PassWdAuth =>
      context.become(timeoutBehaviour orElse waitingPassAuth)
      sendToDB(pwa)

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in auth actor")
  }

  def waitingRegister: Receive = {
    case DBReply(succ, id) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpIdReply(id.get))
        case false => complete(StatusCodes.BadRequest, Error("username has already been taken"))
      }
  }

  def waitingPassAuth: Receive = {
    case DBReply(succ, token) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpTokenReply(token.get))
        case false => complete(StatusCodes.Unauthorized, Error("Improper authentication credentials"))
      }
  }

  def repeatedHash(n: Int, text: String): String = {
    if (n <= 0) text
    else repeatedHash(n - 1, text.sha256.hex)
  }
}

trait AuthActorCreator {
  this: HttpService =>
  def handleAuth(ctx: RequestContext, msg: Message) = {
    actorRefFactory.actorOf(Props(classOf[AuthActor], ctx, msg))
  }
}
