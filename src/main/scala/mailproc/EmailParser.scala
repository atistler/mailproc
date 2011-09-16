package mailproc {

import akka.actor.Actor
import io.Source
import java.io.ByteArrayInputStream
import javax.mail.{Message, Session => MailSession}
import javax.mail.internet.{InternetAddress, MimeMessage}
import akka.event.EventHandler

class EmailParser(val supportAddresses: Set[String]) extends Actor {
    private val session = MailSession.getDefaultInstance(System.getProperties);

    private val SrSubject = """.*SR \d-(\d{6,}).*""".r

    private val emailFormat = "\tSubject:\t%s\n\tTo:\t\t%s\n\tFrom:\t\t%s"

    def receive = {
      case EmailFile(file) => {
        val lines = Source.fromFile(file).mkString
        val message = new MimeMessage(session, new ByteArrayInputStream(lines.getBytes))

        val from = message.getFrom
        val to = message.getRecipients(Message.RecipientType.TO)
        if (to != null) {
          to.find(
            a => {
              supportAddresses.contains(new InternetAddress(a.toString).getAddress)
            }
          ) match {
            /* To: lw-support or support */
            case Some(a) => {
              val subject = message.getSubject
              val user_type = userCheck ? GetUserType(from(0))
              user_type.get match {
                case LwPrivilegedUser(addr, user) => {
                  subject match {
                    /* Privileged User && Has SR node_id in subject line: add outgoing email node to SR and open SR if necessary */
                    case SrSubject(nodeId) => {
                      EventHandler.info(
                        this, "Email from privileged user (existing SR)\n" + emailFormat.format(
                          subject, from.map(a => new InternetAddress(a.toString).getAddress).mkString(", "),
                          to.map(a => new InternetAddress(a.toString).getAddress).mkString(", ")
                        )
                      )
                      /*
                      ticketHandler ! AddOutgoingEmail(
                        user, nodeId.toInt, subject, to.mkString(","), from(0).toString, message.getContent.toString, file
                      )
                      */

                    }
                    /* PrivilegedUser && No SR node_id in subject line: ignore */
                    case _ =>
                  }
                }
                case LwClientUser(addr, user) => {
                  subject match {
                    /* Client User && Has SR node_id in subject line: add incoming email, open SR if closed */
                    case SrSubject(nodeId) => {
                      EventHandler.info(
                        this, "Email from client user (existing SR : %d)\n" + emailFormat.format(
                          nodeId, subject, from.map(a => new InternetAddress(a.toString).getAddress).mkString(", "),
                          to.map(a => new InternetAddress(a.toString).getAddress).mkString(", ")
                        )
                      )
                      /*
                      ticketHandler ! AddIncomingEmail(
                        user, nodeId.toInt, subject, to.mkString(","), from(0).toString, message.getContent.toString, file
                      )
                      */

                    }
                    /* Client User && No SR node_id in subject line: create NEW SR */
                    case _ => {
                      EventHandler.info(
                        this, "Email from client user (No existing SR)\n" + emailFormat.format(
                          subject, from.map(a => new InternetAddress(a.toString).getAddress).mkString(", "),
                          to.map(a => new InternetAddress(a.toString).getAddress).mkString(", ")
                        )
                      )
                      /*
                      ticketHandler ! CreateNewSR(user, subject, to.mkString(","), from(0).toString, message.getContent.toString, file)
                      */
                    }
                  }
                }
                /* Unknown user: ignore */
                case LwUnknownUser(addr) => {
                  EventHandler.debug(
                    this, "Email from unknown user\n" + emailFormat.format(
                      subject, from.map(a => new InternetAddress(a.toString).getAddress).mkString(", "),
                      to.map(a => new InternetAddress(a.toString).getAddress).mkString(", ")
                    )
                  )
                }
                case _ => EventHandler.error(this, "Unknown message sent to EmailParser actor")
              }

            }
            /* Not sent to lw-support or support: ignore */
            case None =>
          }
        }
      }
    }
  }
}