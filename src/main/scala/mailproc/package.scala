package mailproc {

import actors.Actor
import io.Source
import java.io.{File, ByteArrayInputStream}
import javax.mail.internet.{MimeMessage, InternetAddress}
import javax.mail.{Address, Message, Session => MailSession}
import logicops.db
import db._

case class EmailFile(file : File)

case class IsLWUser(address : Address)

object LWUserCheck extends Actor {
  def act() {
    val users = Node.find("Generic Container Node", "Name" -> "Acccounts").head.connectees.having(
      connectionTypes = List(ConnectionType.get("Child")), nodeTypes = List(NodeType.get("User"))
    )
    loop {

    }
  }
}

object EmailParser extends Actor {
  val session = MailSession.getDefaultInstance(System.getProperties);
  val LW_SUPPORT_ADDRESSES = Set(
    "lw-support@logicworks.net", "support@logicworks.net"
  )
  val SrSubject = """.*SR \d-(\d{6,}).*""".r

  def act() {
    loop {
      react {
        case EmailFile(file) => {
          val lines = Source.fromFile(file).mkString
          val message = new MimeMessage(session, new ByteArrayInputStream(lines.getBytes))

          val from = message.getFrom
          val to = message.getRecipients(Message.RecipientType.TO)
          if (to != null) {
            to.find(
              a => {
                LW_SUPPORT_ADDRESSES.contains(new InternetAddress(a.toString).getAddress)
              }
            ) match {
              /* To: lw-support or support */
              case Some(a) => {
                val subject = message.getSubject
                println("\nParsing message: ")
                println("  Subject: " + subject)
                println("  From: " + from.map(a => new InternetAddress(a.toString).getAddress).mkString(", "))
                println("  To: " + to.map(a => new InternetAddress(a.toString).getAddress).mkString(", "))
                subject match {
                  /* Has sr node_id in subject line */
                  case SrSubject(nodeId) => {
                    LWUserCheck ! IsLWUser(from(0))
                  }
                  /* No sr node_id in subject line */
                  case _ => {
                  }
                }
              }
              /* Not To: lw-support or support */
              case None =>
            }
          }
        }
      }
    }
  }
}

object DirectoryWatcher {
  val MAX = 5
  val LO_MAIL_DIR = "/Users/atistler/logicops-mail/Maildir/new/"
  var continue = true
  EmailParser.start()

  def watch() {
    while (continue) {
      for (file <- new File(LO_MAIL_DIR).listFiles.take(1000)) {
        EmailParser ! EmailFile(file)
      }
      continue = false
    }
  }
}

object MailProc extends App {
  try {
    DirectoryWatcher.watch()
  } catch {
    case e => println("Trace: " + e.getStackTraceString)
  }
}

}

