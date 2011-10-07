package logicops.mailproc

import akka.actor.Actor
import akka.event.EventHandler
import logicops.db._
import java.util.{UUID, Properties}
import java.sql.Savepoint
import java.io.{ByteArrayOutputStream, File}
import org.apache.commons.io.FileUtils
import javax.mail._
import internet.{MimeBodyPart, MimeMultipart, MimeMessage, InternetAddress}
import java.lang.IllegalStateException

class TicketHandler extends Actor {

  private val SR_CLOSED_STATUSES = Set("Closed", "Cancelled")

  implicit def Node2HasSRQ(node : Node) = {
    new {
      def serviceRequestQueue = node.connectors.having("Child", "Service Request Queue").headOption match {
        case Some((nodeid : Int, node : Node)) => Some(node)
        case None => None
      }
    }
  }

  override def preStart() {
    EventHandler.debug(this, "In preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
    EventHandler.debug(this, "Unassigned SRQ: " + unassignedSrq)
  }

  override def preRestart(reason : Throwable) {
    EventHandler.error(reason, this, "Actor %s %s, restarted".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.debug(this, "In postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
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


  private def addToStorage(emailNode : Node, body : String, file : File) {
    val base_path = "%s/%d/%d/".format(
      PROPS.getProperty("attachments-directory"), emailNode.id.get % 100, emailNode.id.get
    )
    val base_path_dir = new File(base_path)
    if (!base_path_dir.isDirectory)
      base_path_dir.mkdir()
    val body_file = new File(base_path_dir, "body.txt")
    FileUtils.writeStringToFile(body_file, body)
    val raw_file = new File(base_path_dir, "raw.txt")
    FileUtils.copyFile(file, raw_file, true)
    emailNode.setAttr("Email Path", body_file.getAbsolutePath)
    emailNode.setAttr("Email Body Path", raw_file.getAbsolutePath)
  }

  private def buildEmail(emailNode : Node, subject : String, to : String) : Node = {
    emailNode.setAttr("Name", subject)
      .setAttr("Computed Recipients", to)
      .setAttr("Email Subject", subject)
  }



  private def withRollback(f : => Unit)(implicit savepoint : Savepoint, file : File) {
    try {
      f
    } catch {
      case e : Exception => {
        Database.getConnection.rollback(savepoint)
        fileHandler ! FileFailed(file, e)
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
            val email = buildEmail(Node.createFrom("Outgoing Email"), subject, to)
              .connect("Created By", user)
              .connect("Child", sr_node)
            EventHandler.info(
              this, "Creating new Outgoing Email under: %s, created by: %s".format(
                sr_node.valueOf("Name").get, user.valueOf("Name").get
              )
            )
            sr_node.valueOf("Service Request Status") match {
              case Some(status) => {
                if (SR_CLOSED_STATUSES.contains(status)) {
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
                  emailSender ! SendReopenedEmail(user, sr_node, subject)
                }
                addToStorage(email, body, file)
                fileHandler ! FileSuccess(file)
              }
              case None => {
                fileHandler ! FileFailed(
                  file, new IllegalStateException("Could not get status for SR: %d".format(sr_node_id))
                )
              }
            }
          }
          case None => {
            fileHandler ! FileFailed(
              file, new IllegalStateException("Could not fetch SR denoted in subject line: %d".format(sr_node_id))
            )
          }
        }
      }
    }
    case AddIncomingEmail(user, sr_node_id, subject, to, from, body, file, hidden) => {
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
            val email = buildEmail(Node.createFrom("Incoming Email"), subject, to)
              .connect("Created By", user)
              .connect("Child", sr_node)

            if (hidden) {
              email.setAttr("Visibility Level", "-128")
            }
            sr_node.valueOf("Service Request Status") match {
              case Some(status) => {
                if (SR_CLOSED_STATUSES.contains(status)) {
                  EventHandler.info(
                    this, "Reopening closed SR: %s".format(
                      sr_node.valueOf("Name").get

                    )
                  )
                  sr_node.setAttr("Service Request Status", "Open")
                  sr_node.connectees.having("Assigned To", "User").headOption match {
                    case Some((node_id, user_node)) => {
                      emailSender ! SendReopenedEmail(user_node, sr_node, subject)
                    }
                    case None =>
                  }
                }
                addToStorage(email, body, file)
                fileHandler ! FileSuccess(file)
              }
              case None => {
                fileHandler ! FileFailed(
                  file, new IllegalStateException("Could not get sr status of sr node_id: %d".format(sr_node_id))
                )
              }
            }
          }
          case None => {
            fileHandler ! FileFailed(
              file, new IllegalStateException("Could not get sr node_id in subject line: %d".format(sr_node_id))
            )
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
        val email = buildEmail(Node.createFrom("Incoming Email"), subject, to)
          .connect("Created By", user)
          .connect("Child", sr_node)

        addToStorage(email, body, file)
        emailSender ! SendConfirmEmail(user, sr_node, subject)
        fileHandler ! FileSuccess(file)
      }
    }
  }
}
