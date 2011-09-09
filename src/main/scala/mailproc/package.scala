package mailproc {

import io.Source
import java.io.{File, ByteArrayInputStream}
import javax.mail.{Address, Message, Session => MailSession}
import logicops.db._
import akka.actor.Actor._
import akka.event.EventHandler
import akka.config.Supervision._
import akka.actor.{Actor, Supervisor, MaximumNumberOfRestartsWithinTimeRangeReached, ActorRef}
import collection.mutable
import javax.mail.internet.{InternetAddress, MimeMessage}

object MailProc extends App {
  val supervisor = Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 5000),
      Supervise(
        actorOf[UserCheck],
        Permanent
      ) ::
        Supervise(
          actorOf[EmailParser],
          Permanent
        ) ::
        Nil
    )
  )

  case class IsLWUser(address : Address)

  class UserCheck extends Actor {

    val lw_account = Node.find("Account", "Name" -> "Logicworks").head
    val users = lw_account.connectors.having(
      connectionTypes = List(ConnectionType.get("Child")), nodeTypes = List(NodeType.get("User"))
    )

    var email_users = new mutable.HashMap[String, Node] with mutable.SynchronizedMap[String, Node]
    for (user <- users) {
      user.attr("Email Address") match {
        case Some(na) => email_users(na.value.trim()) = user
        case None => println("Unknown: ")
      }
      user.attr("Alternate Email Address") match {
        case Some(na) => email_users(na.value.trim()) = user
        case None => println("Unknown: ")
      }
    }
    println(email_users)

    def receive = {
      case IsLWUser(addr) => {
        if ( email_users.contains(new InternetAddress(addr.toString).getAddress) ) {
          println("   !!! FROM: " + new InternetAddress(addr.toString).getAddress + " is LW User")
        } else {
          println("   !!! FROM: " + new InternetAddress(addr.toString).getAddress + " is not LW User")
        }
      }
      case _ =>  println("Invalid request in UserCheck")
    }
  }

  case class EmailFile(file : File)

  class EmailParser extends Actor {
    private val session = MailSession.getDefaultInstance(System.getProperties);
    private val LW_SUPPORT_ADDRESSES = Set(
      "lw-support@logicworks.net", "support@logicworks.net"
    )
    private val SrSubject = """.*SR \d-(\d{6,}).*""".r

    def receive = {
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
                  userCheck ! IsLWUser(from(0))
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


  val userCheck = actorOf(new UserCheck).start()

  val emailParser = actorOf(new EmailParser).start()

  object DirectoryWatcher {
    val MAX = 5
    private val LO_MAIL_DIR = "/Users/atistler/logicops-mail/Maildir/new/"
    private var continue = true

    def watch() {
      while (continue) {
        for (file <- new File(LO_MAIL_DIR).listFiles.take(1000)) {
          emailParser ! EmailFile(file)
        }
        continue = false
      }
    }
  }

  try {
    DirectoryWatcher.watch()
  } catch {
    case e => println("Trace: " + e.getStackTraceString)
  }
}

}

