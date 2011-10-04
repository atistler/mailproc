package logicops.mailproc

import akka.actor.Actor
import io.Source
import akka.event.EventHandler
import logicops.db._
import collection.mutable
import java.io.{IOException, UnsupportedEncodingException, ByteArrayInputStream}
import org.jsoup._
import safety.{Whitelist, Cleaner}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{MessagingException, Address, Multipart, Part, Message, Session => MailSession}

class EmailParser(val supportAddresses : Set[String]) extends Actor {

  import EmailParser.prettyAddress

  override def preStart() {
    EventHandler.info(this, "preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def preRestart(reason : Throwable) {
    EventHandler.info(this, "preRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.info(this, "postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  def receive = {
    case EmailFile(file) => {
      EventHandler.debug(this, "EmailParser processing file: %s".format(file.getName))
      val lines = Source.fromFile(file).mkString
      val message = new MimeMessage(EmailParser.session, new ByteArrayInputStream(lines.getBytes))

      val from = message.getFrom
      val to = message.getRecipients(Message.RecipientType.TO)

      val content = EmailParser.getPlainTextContent(EmailParser.findMimeTypes(message, "text/plain", "text/html"))
      if (to == null) {
        EventHandler.error(this, "'To:' header does not exist in message: %s, ignoring this email".format(file))
        fileHandler ! FileIgnored(file)
      } else {
        to.find(a => supportAddresses.contains(prettyAddress(a))) match {
          /* To: lw-support or support */
          case Some(a) => {
            val subject = message.getSubject
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
                    EventHandler.info(this, "Sending msg to TicketHandler: %s".format(msg))
                    ticketHandler ! msg
                  }
                  /* PrivilegedUser && No SR node_id in subject line: ignore */
                  case _ =>
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
                    EventHandler.info(this, "Sending msg to TicketHandler: %s".format(msg))
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
                    EventHandler.info(this, "Sending msg to TicketHandler: %s".format(msg))
                    ticketHandler ! msg
                  }
                }
              }
              /* Unknown user: ignore */
              case LwUnknownUser(addr) => {
                EventHandler.debug(
                  this, "Email from unknown user .. skipping\n" + EmailParser.emailFormat.format(
                    subject, prettyAddress(from), prettyAddress(to)
                  )
                )
                fileHandler ! FileIgnored(file)
              }
              case _ => {
                EventHandler.error(this, "Unknown message sent to EmailParser actor")
                fileHandler ! FileIgnored(file)
              }
            }
          }
          /* Not sent to lw-support or support: ignore */
          case None => {
            fileHandler ! FileIgnored(file)
          }
        }
      }
    }
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
          this, "UnsupportedEncodingException: " + e.getStackTraceString
        )
        case e : MessagingException => EventHandler.error(this, "MessagingException: " + e.getStackTraceString)
        case e : IOException => EventHandler.error(this, "IOException: " + e.getStackTraceString)
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
