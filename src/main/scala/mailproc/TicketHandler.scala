package mailproc {

import akka.actor.Actor
import akka.event.EventHandler
import logicops.db._
import java.util.{UUID, Properties}
import javax.mail._
import internet.{MimeMessage, InternetAddress}
import java.sql.Savepoint
import java.lang.IllegalStateException
import java.io.File

class TicketHandler extends Actor {

  implicit def Node2HasSRQ(node : Node) = new HasSRQ(node)

  class HasSRQ(node : Node) {
    def serviceRequestQueue = node.connectors.having("Child", "Service Request Queue").headOption match {
      case Some((nodeid : Int, node : Node)) => Some(node)
      case None => None
    }
  }

  override def preStart() {
    EventHandler.info(this, "preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
    EventHandler.debug(this, "Unassigned SRQ: " + unassignedSrq)
  }

  override def preRestart(reason : Throwable) {
    EventHandler.info(this, "preRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.info(this, "postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  private lazy val unassignedSrq = Node.find("User", "Name" -> "Unassigned").headOption match {
    case Some(node : Node) => {
      node.serviceRequestQueue match {
        case Some(srq : Node) => {
          srq
        }
        case None => throw new NoSuchElementException(
          "Could not find SRQ under user: %s".format(node.valueOf("Name").get)
        )
      }
    }
    case None => throw new NoSuchElementException("Could not find 'Unassigned' user")
  }


  private def buildEmail(emailNode : Node, subject : String, to : String) : Node = {
    emailNode.setAttr("Name", subject)
      .setAttr("Computed Recipients", to)
      .setAttr("Email Subject", subject)
      .setAttr("Email Path", "%d/email_%d.txt".format(emailNode.id.get % 100, emailNode.id.get))
  }

  private def sendConfirmEmail(user : Node, serviceRequest : Node) {
    def getAddresses(addrs : Option[String]*) = {
      addrs.foldLeft(List.empty[Address]) {
        (l, a) => InternetAddress.parse(a.getOrElse("")).toList ::: l
      }
    }

    def getConfirmLink(serviceRequest : Node) = {
      val confirm_token = UUID.randomUUID().toString
      serviceRequest.setAttr("Confirmation Token", confirm_token)
      "%s?token=%s".format(PROPS.getProperty("confirm-url"), confirm_token)
    }

    def buildMessage(session : Session, user : Node) = {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(PROPS.getProperty("confirm-email-from")))
      message.setReplyTo(InternetAddress.parse(PROPS.getProperty("confirm-email-replyto")).toArray)
      val to = getAddresses(user.valueOf("Email Address"), user.valueOf("Alternate Email Address"))
      val confirm_link = getConfirmLink(serviceRequest)
      if (isProduction) {
        message.setRecipients(Message.RecipientType.TO, to.toArray)
      } else {
        message.setRecipients(
          Message.RecipientType.TO,
          InternetAddress.parse(PROPS.getProperty("test-mode-recipient")).map(_.asInstanceOf[Address]).toArray
        )
      }
      message.setSubject("Confirmation Email")
      message.setText("Link: %s".format(confirm_link))
      message
    }

    val props = new Properties()
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    val session = Session.getInstance(props)
    session.getTransport("smtp").connect(
      PROPS.getProperty("smtp-host"), PROPS.getProperty("smtp-port").toInt, PROPS.getProperty("smtp-user"),
      PROPS.getProperty("smtp-pass")
    );
    val message = buildMessage(session, user)
    Transport.send(message);
  }

  EventHandler.info(this, "TicketHandler constructor initialized")

  private def withRollback(f : => Unit)(implicit savepoint : Savepoint, file : File) {
    try {
      f
    } catch {
      case e : Exception => {
        Database.getConnection.rollback(savepoint)
        fileHandler ! FileFailed(file,e)
      }
    }
  }

  def receive = {
    case AddOutgoingEmail(user, sr_node_id, subject, to, from, body, file) => {
      implicit val savepoint = Database.getConnection.setSavepoint()
      implicit val email_file = file
      withRollback {
        Node.getOption(sr_node_id) match {
          case Some(sr_node) => {
            buildEmail(Node.createFrom("Outgoing Email"), subject, to)
              .connect("Created By", user)
              .connect("Child", sr_node)
            EventHandler.info(
              this, "Creating new Outgoing Email under: %s, created by: %s".format(
                sr_node.valueOf("Name").get, user.valueOf("Name").get
              )
            )
            sr_node.valueOf("Service Request Status") match {
              case Some(status) => {
                if (status == "Closed" || status == "Cancelled") {
                  EventHandler.info(
                    this, "Reopening closed SR: %s, assigning to: %s".format(
                      sr_node.valueOf("Name").get, user.valueOf("Name").get
                    )
                  )
                  sr_node.setAttr("Service Request Status", "Open")
                    .connectors.having(List(ConnectionType.get("Assigned To")), List(NodeType.get("User")))
                    .foreach {
                    case (nid : Int, node : Node) => sr_node.break("Assigned To", node)
                  }
                  sr_node.connect("Assigned To", user)
                    .connect("Child", user.serviceRequestQueue.get)
                }
                fileHandler ! FileSuccess(file)
              }
              case None => {
                val msg = "Could not get status for SR: %d".format(sr_node_id)
                throw new IllegalStateException(msg)
              }
            }
          }
          case None => {
            val msg = "Could not fetch SR denoted in subject line: %d".format(sr_node_id)
            throw new IllegalStateException(msg)
          }
        }
      }
    }
    case AddIncomingEmail(user, sr_node_id, subject, to, from, body, file) => {
      implicit val savepoint = Database.getConnection.setSavepoint()
      implicit val email_file = file
      withRollback {
        Node.getOption(sr_node_id) match {
          case Some(sr_node) => {
            EventHandler.info(
              this, "Creating new Incoming Email under: %s, created by: %s".format(
                sr_node.valueOf("Name").get, user.valueOf("Name").get
              )
            )
            buildEmail(Node.createFrom("Incoming Email"), subject, to)
              .connect("Created By", user)
              .connect("Child", sr_node)
            sr_node.valueOf("Service Request Status") match {
              case Some(status) => {
                if (status == "Closed" || status == "Cancelled") {
                  EventHandler.info(
                    this, "Reopening closed SR: %s, assigning to: %s".format(
                      sr_node.valueOf("Name").get, user.valueOf("Name").get
                    )
                  )
                  sr_node.setAttr("Service Request Status", "Open")
                    .connectors.having(List(ConnectionType.get("Assigned To")), List(NodeType.get("User")))
                    .foreach {
                    case (nid : Int, n : Node) => sr_node.break("Assigned To", n)
                  }
                  sr_node.connect("Assigned To", user)
                    .connect("Child", user.serviceRequestQueue.get)
                }
                fileHandler ! FileSuccess(file)
              }
              case None => {
                val msg = "Could not get sr status of sr node_id: %d\".format(sr_node_id)"
                throw new IllegalStateException(msg)
              }
            }
          }
          case None => {
            val msg = "Could not get sr node_id in subject line: %d".format(sr_node_id)
            throw new IllegalStateException(msg)
          }
        }
      }
    }
    case CreateNewSR(user, subject, to, from, body, file) => {
      implicit val savepoint = Database.getConnection.setSavepoint()
      implicit val email_file = file
      withRollback {
        val sr_node = Node.createFrom("Client Email SR")
        sr_node.setAttr("Abstract", subject)
          .setAttr("Name", "SR 3-%d".format(sr_node.id.get))
          .setAttr("Service Request Status", "Unconfirmed")
          .connect("Child", unassignedSrq)
          .connect("Assigned To", unassignedSrq)
          .connect("Child", user)
        EventHandler.info(
          this, "Creating new SR: %s, assigning to: %s".format(sr_node.valueOf("Name").get, user.valueOf("Name").get)
        )
        EventHandler.info(
          this, "Creating new Incoming Email under: %s, created by: %s".format(
            sr_node.valueOf("Name").get, user.valueOf("Name").get
          )
        )
        buildEmail(Node.createFrom("Incoming Email"), subject, to)
          .connect("Created By", user)
          .connect("Child", sr_node)

        sendConfirmEmail(user, sr_node)
        fileHandler ! FileSuccess(file)
      }
    }
  }
}
}
