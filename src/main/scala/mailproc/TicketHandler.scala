package mailproc {

import akka.actor.Actor
import akka.event.EventHandler
import logicops.db._
import javax.mail._
import internet._
import java.util.{UUID, Properties}


class TicketHandler extends Actor {

  implicit def Node2HasSRQ(node : Node) = new HasSRQ(node)

  class HasSRQ(node : Node) {
    def serviceRequestQueue = node.connectors.having(ConnectionType.get("Child"), NodeType.get("Service Request Queue")).headOption
  }

  private lazy val unassignedSrq = Node.find("User", "Name" -> "Unassigned").headOption match {
    case Some(node : Node) => node.serviceRequestQueue match {
      case Some(srq: Node) => srq
      case None => throw new TicketHandlerException("Could not find SRQ under user: %s".format(node.valueOf("Name").get))
    }
    case None => throw new TicketHandlerException("Could not find 'Unassigned' user")
  }

  class TicketHandlerException(msg : String) extends RuntimeException(msg)



  private def buildEmail(emailNode : Node, subject : String, to : String) : Node = {
    emailNode.setAttr("Name", subject)
      .setAttr("Computed Recipients", to)
      .setAttr("Email Subject", subject)
      .setAttr("Email Path", "%d/email_%d.txt".format(emailNode.id.get % 100, emailNode.id.get))
  }

  private def sendConfirmEmail(user : Node, serviceRequest : Node) {
    def getAddresses(addrs : Option[String]*) = {
      addrs.foldLeft(List.empty[Address]) { (l,a) => InternetAddress.parse(a.getOrElse("")).toList ::: l}
    }

    def getConfirmLink(serviceRequest : Node) = {
      val confirm_token = UUID.randomUUID().toString
      serviceRequest.setAttr("Confirmation Token", confirm_token)
      "%s?token=%s".format(PROPS.getProperty("confirm-url"), confirm_token)
    }

    def buildMessage(session : Session, user : Node) = {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(PROPS.getProperty("confirm-email.from")))
      message.setReplyTo(InternetAddress.parse(PROPS.getProperty("confirm-email.replyto")).toArray)
      val to = getAddresses(user.valueOf("Email Address"), user.valueOf("Alternate Email Address"))
      val confirm_link = getConfirmLink(serviceRequest)
      message.setRecipients(Message.RecipientType.TO, to.toArray);
      message.setSubject("Confirmation Email");
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

  def receive = {
    case AddOutgoingEmail(user, sr_node_id, subject, to, from, body, file) => {
      Node.getOption(sr_node_id) match {
        case Some(sr_node) => {
          val email_node = buildEmail(Node.createFrom("Outgoing Email").save().get, subject, to)
            .connect("Created By", user)
            .connect("Child", sr_node)
          sr_node.valueOf("Service Request Status") match {
            case Some(status) => {
              if (status == "Closed" || status == "Cancelled") {
                sr_node.setAttr("Service Request Status", "Open")
                  .connectors.having(List(ConnectionType.get("Assigned To")), List(NodeType.get("User")))
                  .foreach {
                  case (nid : Int, n : Node) => sr_node.break("Assigned To", n)
                }
                sr_node.connect("Assigned To", user)
                  .connect("Child", user.serviceRequestQueue.head._2)
              }
            }
            case None => EventHandler.error(this, "Could not get sr status of sr node_id: %d".format(sr_node_id))
          }
        }
        case None => EventHandler.error(this, "Could not get sr node_id in subject line: %d".format(sr_node_id))
      }
    }
    case AddIncomingEmail(user, sr_node_id, subject, to, from, body, file) => {
      Node.getOption(sr_node_id) match {
        case Some(sr_node) => {
          val email_node = buildEmail(Node.createFrom("Incoming Email").save().get, subject, to)
            .connect("Created By", user)
            .connect("Child", sr_node)
          sr_node.valueOf("Service Request Status") match {
            case Some(status) => {
              if (status == "Closed" || status == "Cancelled") {
                sr_node.setAttr("Service Request Status", "Open")
                  .connectors.having(List(ConnectionType.get("Assigned To")), List(NodeType.get("User")))
                  .foreach {
                  case (nid : Int, n : Node) => sr_node.break("Assigned To", n)
                }
                sr_node.connect("Assigned To", user)
                  .connect("Child", user.serviceRequestQueue.head._2)
              }
            }
            case None => EventHandler.error(this, "Could not get sr status of sr node_id: %d".format(sr_node_id))
          }
        }
        case None => EventHandler.error(this, "Could not get sr node_id in subject line: %d".format(sr_node_id))
      }
    }
    case CreateNewSR(user, subject, to, from, body, file) => {
      val sr_node = Node.createFrom("Client Email SR").save().get
      sr_node.setAttr("Abstract", subject)
        .setAttr("Name", "SR 3-%d".format(sr_node.id.get))
        .setAttr("Service Request Status", "Unconfirmed")
        .connect("Child", unassignedSrq)
        .connect("Assigned To", unassignedSrq)
        .connect("Child", user)

      val email_node = buildEmail(Node.createFrom("Incoming Email").save().get, subject, to)
        .connect("Created By", user)
        .connect("Child", sr_node)


    }
  }

}

}