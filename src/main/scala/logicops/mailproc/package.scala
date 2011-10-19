package logicops {

import java.io.File
import javax.mail.Address
import logicops.db._
import akka.actor.Actor._
import logicops.utils._

package object mailproc {

  sealed trait MpMessage

  case class GetUserType(address : Address) extends MpMessage

  case class LwPrivilegedUser(address : String, user : Node) extends MpMessage

  case class LwClientUser(address : String, user : Node) extends MpMessage

  case class LwUnknownUser(address : String) extends MpMessage

  case class Reload() extends MpMessage

  case class ReloadComplete() extends MpMessage

  case class ProcessFiles(num : Int) extends MpMessage

  case class DoneProcessingFiles() extends MpMessage

  case class StartWatch() extends MpMessage

  case class EmailFile(file : File) extends MpMessage

  case class AddOutgoingEmail(
    user : Node, srNodeId : Int, subject : String, to : String, from : String, body : String, file : File
    ) extends MpMessage {
    override def toString = "%s(\n\tUser: %s, SR: %d, To: %s, From: %s,\n\tSubject: %s,\n\tBody: %s\n\tFile: %s\n\n)".format(
      getClass.getName, user.valueOf("Name").get, srNodeId, to, from, subject, body.take(30), file
    )
  }

  case class AddIncomingEmail(
    user : Node, srNodeId : Int, subject : String, to : String, from : String, body : String, file : File, hidden : Boolean = false
    ) extends MpMessage {
    override def toString = "%s(\n\tUser: %s, SR: %d, To: %s, From: %s,\n\tSubject: %s,\n\tBody: %s\n\tFile: %s\n)".format(
      getClass.getName, user.valueOf("Name").get, srNodeId, to, from, subject, body.take(30), file
    )
  }

  case class CreateNewSR(
    user : Node, subject : String, to : String, from : String, body : String, file : File
    ) extends MpMessage {
    override def toString = "%s(\n\tUser: %s, To: %s, From: %s,\n\tSubject: %s,\n\tBody: %s\n\tFile: %s\n)".format(
      getClass.getName, user.valueOf("Name").get, to, from, subject, body.take(30), file
    )
  }

  case class SendReopenedEmail(user : Node, serviceRequest : Node, subject : String)

  case class SendConfirmEmail(user : Node, serviceRequest : Node, subject : String)

  case class FileSuccess(file : File) extends MpMessage

  case class FileFailed(file : File, e : Exception) extends MpMessage

  case class FileIgnored(file : File) extends MpMessage


}

}

