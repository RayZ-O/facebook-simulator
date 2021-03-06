package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._
import java.util.UUID
import akka.actor.actorRef2Scala
import edu.ufl.dos15.fbapi.FBMessage._
import edu.ufl.dos15.crypto._

case class NonceInfo(id: String, expireOn: Long)
case class TokenInfo(id: String, expireOn: Long)

class AuthDB extends Actor with ActorLogging {
  val dbNo = "001"
  import scala.collection.mutable.HashMap
  private var pubKeyDB = new HashMap[String, Array[Byte]]
  import scala.collection.mutable.LinkedHashMap
  private var nonceDB = new LinkedHashMap[String, NonceInfo]
  private var tokenDB = new LinkedHashMap[String, TokenInfo]

  import context.dispatcher
  val tick = context.system.scheduler.schedule(1.second, 60.second, self, Tick)
  var sequenceNum = 0

  def receive = {
    case Register(pub) =>
      val id = generateId()
      pubKeyDB += (id -> pub)
      sender ! DBStrReply(true, Some(id), Some(pub))

    case GetNonce(id) =>
      pubKeyDB.get(id) match {
        case Some(pub) =>
          val nonce = Crypto.generateNonce
          val expire = System.currentTimeMillis + 300000L
          nonceDB += (nonce -> NonceInfo(id, expire))
          sender ! DBStrReply(true, Some(nonce), Some(pub))
        case None => sender ! DBStrReply(false)
      }

    case CheckNonce(n, sign) =>
      nonceDB.get(n) match {
        case Some(ni) =>
          nonceDB -= n
          pubKeyDB.get(ni.id) match {
            case Some(pub) =>
              if (Crypto.RSA.verify(n, sign, pub)) {
                val token = produceToken(ni.id)
                sender ! DBStrReply(true, Some(token), Some(pub))
              } else {
                sender ! DBStrReply(false)
              }
            case None => sender ! DBStrReply(false)
          }

        case None =>
          sender ! DBStrReply(false)
      }

    case TokenAuth(token) =>
      tokenDB.get(token) match {
        case Some(ti) => sender ! DBStrReply(true, Some(ti.id))
        case None => sender ! DBStrReply(false)
      }

    case DBTestToken(id, token) =>
      val expire = System.currentTimeMillis + 3600000L
      tokenDB += (token -> TokenInfo(id, expire))

    case Tick =>
      nonceDB.dropWhile(t => t._2.expireOn < System.currentTimeMillis())
      tokenDB.dropWhile(t => t._2.expireOn < System.currentTimeMillis())
  }

  def produceToken(id: String) = {
    val token = Crypto.generateToken()
    val expire = System.currentTimeMillis + 3600000L
    tokenDB += (token -> TokenInfo(id, expire))
    token
  }

  def generateId() = {
    sequenceNum += 1
    dbNo + System.currentTimeMillis().toString + sequenceNum
  }
}
