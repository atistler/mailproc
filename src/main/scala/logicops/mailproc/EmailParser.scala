package logicops.mailproc

import akka.actor.Actor
import akka.event.EventHandler
import logicops.db._
import collection.mutable
import org.jsoup._
import safety.{Whitelist, Cleaner}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{MessagingException, Address, Multipart, Part, Message, Session => MailSession}
import java.io.{File, FileInputStream, IOException, UnsupportedEncodingException}


class EmailParser(val supportAddresses : Set[String], val internalAddresses : Set[String]) extends Actor {

  import EmailParser.prettyAddress

  override def preStart() {
    EventHandler.debug(this, "In preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def preRestart(reason : Throwable) {
    EventHandler.error(reason, this, "Actor %s %s, restarted".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.debug(this, "In postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  private def processInternalEmail(message : MimeMessage, to : Array[Address], from : Array[Address], file : File) {
    EventHandler.debug(this, "Sent to internal address, from: " + from(0))
    val subject = message.getSubject
    val content = EmailParser.getPlainTextContent(EmailParser.findMimeTypes(message, "text/plain", "text/html"))
    val user_type = userCheck ? GetUserType(from(0))
    user_type.get match {
      case LwPrivilegedUser(addr, user) => {
        subject match {
          case EmailParser.SrSubject(nodeId) => {
            EventHandler.info(
              this,
              "Internal email from privileged user %s (existing SR : %s)\n".format(user.valueOf("Name").get, nodeId)
                + EmailParser.emailFormat.format(subject, prettyAddress(from), prettyAddress(to))
            )

            val msg = AddIncomingEmail(
              user, nodeId.toInt, subject, prettyAddress(to), prettyAddress(from(0)), content, file, true
            )
            EventHandler.debug(this, "Sending msg to TicketHandler: %s".format(msg))
            ticketHandler ! msg
          }
          case _ => {
            EventHandler.debug(this, "Internal email from privileged user, no SR in subject line, ignoring")
            fileHandler ! FileIgnored(file)
          }
        }
      }
      case _ => {
        EventHandler.debug(this, "Internal email sent from non-privileged or unknown user, ignoring")
        fileHandler ! FileIgnored(file)
      }
    }
  }

  private def processSupportEmail(message : MimeMessage, to : Array[Address], from : Array[Address], file : File) {
    EventHandler.debug(this, "Sent to support address, from: " + from(0))
    val subject = message.getSubject
    val content = EmailParser.getPlainTextContent(EmailParser.findMimeTypes(message, "text/plain", "text/html"))
    val user_type = userCheck ? GetUserType(from(0))
    user_type.get match {
      case LwPrivilegedUser(addr, user) => {
        subject match {
          /* Privileged User && Has SR node_id in subject line: add outgoing email node to SR and open SR if necessary */
          case EmailParser.SrSubject(nodeId) => {
            EventHandler.info(
              this,
              "Email from privileged user %s (existing SR : %s)\n".format(user.valueOf("Name").get, nodeId)
                + EmailParser.emailFormat.format(subject, prettyAddress(from), prettyAddress(to))
            )

            val msg = AddOutgoingEmail(
              user, nodeId.toInt, subject, prettyAddress(to), prettyAddress(from(0)), content, file
            )
            EventHandler.debug(this, "Sending msg to TicketHandler: %s".format(msg))
            ticketHandler ! msg
          }
          /*
          *  PrivilegedUser && No SR node_id in subject line: ignore
          *  Note: This case could be used for API-like functionality to
          *  create SRs directly from emails sent from staff
          */
          case _ => {
            EventHandler.debug(this, "Email from privileged user, no SR in subject line, ignoring")
            fileHandler ! FileIgnored(file)
          }
        }
      }
      case LwClientUser(addr, user) => {
        subject match {
          /* Client User && Has SR node_id in subject line: add incoming email, open SR if closed */
          case EmailParser.SrSubject(nodeId) => {
            EventHandler.info(
              this, "Email from client user %s (existing SR : %s)\n".format(user.valueOf("Name").get, nodeId)
                + EmailParser.emailFormat.format(subject, prettyAddress(from), prettyAddress(to))
            )
            val msg = AddIncomingEmail(
              user, nodeId.toInt, subject, prettyAddress(to), prettyAddress(from(0)), content, file
            )
            EventHandler.debug(this, "Sending msg to TicketHandler: %s".format(msg))
            ticketHandler ! msg
          }
          /* Client User && No SR node_id in subject line: create NEW SR */
          case _ => {
            EventHandler.info(
              this, "Email from client user %s (No existing SR)\n".format(user.valueOf("Name").get)
                + EmailParser.emailFormat.format(subject, prettyAddress(from), prettyAddress(to))
            )
            val msg = CreateNewSR(
              user, subject, prettyAddress(to), prettyAddress(from(0)), content, file
            )
            EventHandler.debug(this, "Sending msg to TicketHandler: %s".format(msg))
            ticketHandler ! msg
          }
        }
      }
      /* Unknown user: ignore */
      case LwUnknownUser(addr) => {
        EventHandler.debug(
          this, "Email from unknown user, ignoring\n" + EmailParser.emailFormat.format(
            subject, prettyAddress(from), prettyAddress(to)
          )
        )
        fileHandler ! FileIgnored(file)
      }
      case _ => {
        EventHandler.error(this, "Unknown message sent to EmailParser actor, file: %s".format(file))
        fileHandler ! FileIgnored(file)
      }
    }
  }

  def receive = {
    case EmailFile(file) => {
      EventHandler.debug(this, "EmailParser processing file: %s".format(file.getName))

      val message = new MimeMessage(EmailParser.session, new FileInputStream(file))
      val from = message.getFrom
      val to = message.getRecipients(Message.RecipientType.TO)

      if (to == null) {
        EventHandler.error(this, "'To:' header does not exist in message: %s, ignoring this email".format(file))
        fileHandler ! FileIgnored(file)
      } else {
        to.find(a => supportAddresses.contains(prettyAddress(a))) match {
          /* To: lw-support or support */
          case Some(addr) => {
            processSupportEmail(message, to, from, file)
          }

          case None => {
            to.find(a => internalAddresses.contains(prettyAddress(a))) match {
              case Some(addr) => {
                processInternalEmail(message, to, from, file)
              }
              /* Not sent to support or internal: ignore */
              case None => {
                EventHandler.debug(this, "Message not sent to support address, ignoring")
                fileHandler ! FileIgnored(file)
              }
            }
          }

        }
      }
    }
    case _ => EventHandler.error(this, "Unknown message sent to EmailParser actor")
  }
}

object EmailParser {

  def getPlainTextContent(parts : mutable.Map[String, Part]) : String = {
    parts.get("text/plain") match {
      case Some(part) => part.getContent.toString
      case None => parts.get("text/html") match {
        case Some(part) => {
          val doc = Jsoup.parse(part.getContent.toString)
          val cleaner = new Cleaner(Whitelist.simpleText().addTags("br"))
          val clean_doc = cleaner.clean(doc)
          clean_doc.body().html()
        }
        case None => throw new MessagingException("Could not find 'text/plain' or 'text/html' email part")
      }
    }
  }

  def findMimeTypes(part : Part, mimeTypes : String*) : mutable.Map[String, Part] = {
    val contentTypes = mutable.Map.empty[String, Part]
    def findMimeTypeHelper(part : Part, mimeTypes : String*) {
      try {
        if (part.isMimeType("multipart/*")) {
          val mp = part.getContent.asInstanceOf[Multipart]
          for (i <- 0 until mp.getCount) {
            val p = mp.getBodyPart(i)
            findMimeTypeHelper(p, mimeTypes : _*)
          }
        } else {
          for (mt <- mimeTypes) {
            if (part.isMimeType(mt)) {
              contentTypes += mt -> part
            }
          }
        }
      } catch {
        case e : UnsupportedEncodingException => EventHandler.error(
          e, this, "UnsupportedEncodingException parsing email"
        )
        case e : MessagingException => EventHandler.error(e, this, "MessagingException parsing email")
        case e : IOException => EventHandler.error(e, this, "IOException parsing email")
      }
    }
    findMimeTypeHelper(part, mimeTypes : _*)
    contentTypes
  }

  def prettyAddress(addresses : Array[Address]) : String = {
    addresses.map(a => new InternetAddress(a.toString).getAddress.toLowerCase).mkString(", ")
  }

  def prettyAddress(address : Address) : String = {
    new InternetAddress(address.toString).getAddress.toLowerCase
  }

  val session = MailSession.getDefaultInstance(System.getProperties);
  private val SrSubject = """.*SR \d-(\d{6,}).*""".r
  private val emailFormat = "\tSubject:\t%s\n\tFrom:\t\t%s\n\tTo:\t\t%s"
}
