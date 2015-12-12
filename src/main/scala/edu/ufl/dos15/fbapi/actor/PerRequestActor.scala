package edu.ufl.dos15.fbapi.actor

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Props}
import spray.routing.HttpService
import spray.routing.RequestContext
import spray.http.StatusCodes
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import edu.ufl.dos15.fbapi.Json4sProtocol
import edu.ufl.dos15.fbapi.FBMessage._

class PerRequestActor(reqctx: RequestContext, message: Message) extends Actor
    with ActorLogging with Json4sProtocol with RequestHandler {
  val db = context.actorSelection("/user/db")
  val ctx = reqctx

  var objId: String = _
  var putObj: AnyRef = _
  var idList: List[String] = List.empty
  var params: Option[String] = None

  message match {
    case Get(id, p) =>
      context.become(timeoutBehaviour orElse waitingFetch)
      objId = id
      params = p
      sendToDB(Fetch(id))

    case g: GetNewPosts =>
      context.become(timeoutBehaviour orElse waitingNewPosts)
      sendToDB(g)

    case Post(obj) =>
      context.become(timeoutBehaviour orElse waitingInsert)
      sendToDB(Insert(Serialization.write(obj)))

    case EdgePost(id, obj, post) =>
      context.become(timeoutBehaviour orElse waitingInsert)
      sendToDB(EdgeInsert(id, Serialization.write(obj), post))

    case Put(id, obj) =>
      context.become(timeoutBehaviour orElse waitingUpdateFetch)
      objId = id
      putObj = obj
      sendToDB(Fetch(id))

    case d: Delete =>
      context.become(timeoutBehaviour orElse waitingDelete)
      sendToDB(d)

    case msg =>
      throw new UnsupportedOperationException(s"Unsupported Operation $msg in per-request actor")

  }

  def waitingFetch: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true =>
          import org.json4s.JsonDSL._
          val json = (JObject() ~ ("id" -> objId)) merge parse(content.get)
          val result = params match {
            case Some(fields) =>
              val query = fields.split(",")
              query.foldLeft(JObject()) { (res, name) =>
                (res ~ (name -> json \ name)) }
            case None => json
          }
          complete(StatusCodes.OK, result)
        case false => complete(StatusCodes.NotFound, Error("get error"))
      }
  }

  def waitingNewPosts: Receive = {
    case PostReply(posts) =>
      complete(StatusCodes.OK, HttpListReply(posts))
  }

  def waitingInsert: Receive = {
    case DBReply(succ, id) =>
      succ match {
        case true => complete(StatusCodes.Created, HttpIdReply(id.get))
        case false => complete(StatusCodes.BadRequest, Error("post error"))
      }
  }

  def waitingUpdateFetch: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true =>
          context.become(timeoutBehaviour orElse waitingUpdate)
          val value = Extraction.decompose(putObj)
          val json = parse(content.get)
          val updated = compact(render(json merge value))
          db ! Update(objId, updated)

        case false => complete(StatusCodes.NotFound, Error("put-p1 error"))
      }
  }

  def waitingUpdate: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("put-p2 error"))
      }
  }

  def waitingDelete: Receive = {
    case DBReply(succ, content) =>
      succ match {
        case true => complete(StatusCodes.OK, HttpSuccessReply(succ))
        case false => complete(StatusCodes.NotFound, Error("delete error"))
      }
  }
}

trait PerRequestFactory {
  this: HttpService =>
  def handleRequest(ctx: RequestContext, msg: Message) = {
    actorRefFactory.actorOf(Props(classOf[PerRequestActor], ctx, msg))
  }
}

