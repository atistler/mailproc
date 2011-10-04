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
      getClass.getName, user.valueOf("Name").get, srNodeId, to, from, subject, body.substring(0, 30), file
    )
  }

  case class AddIncomingEmail(
    user : Node, srNodeId : Int, subject : String, to : String, from : String, body : String, file : File
    ) extends MpMessage {
    override def toString = "%s(\n\tUser: %s, SR: %d, To: %s, From: %s,\n\tSubject: %s,\n\tBody: %s\n\tFile: %s\n)".format(
      getClass.getName, user.valueOf("Name").get, srNodeId, to, from, subject, body.substring(0, 30), file
    )
  }

  case class CreateNewSR(
    user : Node, subject : String, to : String, from : String, body : String, file : File
    ) extends MpMessage {
    override def toString = "%s(\n\tUser: %s, To: %s, From: %s,\n\tSubject: %s,\n\tBody: %s\n\tFile: %s\n)".format(
      getClass.getName, user.valueOf("Name").get, to, from, subject, body.substring(0, 30), file
    )
  }

  case class FileSuccess(file : File) extends MpMessage

  case class FileFailed(file : File, e : Exception) extends MpMessage

  case class FileIgnored(file : File) extends MpMessage

  private val mailproc_conf = sys.props.get("mailproc.config") match {
    case Some(path) => {
      val file = new File(path)
      if ( file == null || ! file.isFile ) {
        sys.error("System property mailproc.config points to invalid path: '%s'".format(path))
        sys.exit(1)
      } else {
        file
      }
    }
    case None => {
      sys.error("System property mailproc.config must point to the mailproc.properties configuration file")
      sys.exit(1)
    }
  }

  val PROPS = loadConfig(mailproc_conf)

  lazy val isProd = PROPS.getProperty("mode") == "prod"
  lazy val isTest = PROPS.getProperty("mode") == "test"
  lazy val isDev = PROPS.getProperty("mode") == "dev" || !isProd || !isTest

  private[mailproc] val maildir_directory = PROPS.getProperty("maildir-directory")
  private[mailproc] val support_addresses = PROPS.getProperty("support-addresses").split(",").map(_.trim()).toSet
  private[mailproc] val userCheck = actorOf(new UserCheck)
  private[mailproc] val emailParser = actorOf(new EmailParser(support_addresses))
  private[mailproc] val fileHandler = actorOf(new FileHandler(maildir_directory))
  private[mailproc] val ticketHandler = actorOf(new TicketHandler)
  private[mailproc] val directoryWatcher = actorOf(new DirectoryWatcher(maildir_directory))
  private[mailproc] val mailproc = actorOf(new MailProc)
}

}

