package edu.ufl.dos15.db

import akka.actor.{Actor, ActorLogging}
import edu.ufl.dos15.fbapi.FBMessage._

class FriendDB extends Actor with ActorLogging {
  val dbNo = "003"
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.HashSet
  private var friendDB = new HashMap[String, HashSet[String]]
  import scala.collection.mutable.ListBuffer
  private var ownerToList = new HashMap[String, String]
  private var sequenceNum = 0

  def receive = {
    case FetchList(id) =>
      friendDB.get(id) match {
        case Some(hs) => sender ! DBListReply(true, Some(hs.toList))
        case None => sender ! DBListReply(false)
      }

    case GetFriendList(id) =>
      ownerToList.get(id) match {
        case Some(i) => sender ! DBStrReply(true, Some(i))
        case None => sender ! DBListReply(false)
      }

    case FindCommon(id1, id2) =>
      if (friendDB.contains(id1) && friendDB.contains(id2)) {
        val common = friendDB(id1) & friendDB(id2)
        sender ! DBListReply(true, Some(common.toList))
      } else {
        sender ! DBListReply(false)
      }

    case InsertList(ownerId, listId) =>
        friendDB += (listId -> HashSet.empty)
        ownerToList += (ownerId -> listId)
        sender ! DBStrReply(true, Some(listId))

    case UpdateMul(id, idList) =>
      friendDB.get(id) match {
        case Some(fl) =>
          idList foreach { aid =>
            ownerToList.get(aid) match {
              case Some(lid) =>
                friendDB(lid) += id
                fl += aid
              case None => // nothing to do
            }
          }
          sender ! DBSuccessReply(true)
        case None => sender ! DBSuccessReply(false)
      }

    case Delete(objId, ownerId) =>
      if (friendDB.contains(objId)) {
        friendDB -= objId
        ownerToList -= objId
        sender ! DBSuccessReply(true)
      } else {
        sender ! DBSuccessReply(false)
      }

    case DeleteMul(id, idList) =>
      friendDB.get(id) match {
        case Some(fl) =>
          idList foreach { id => fl -= id }
          sender ! DBSuccessReply(true)
        case None => sender ! DBSuccessReply(false)
      }
  }
}
