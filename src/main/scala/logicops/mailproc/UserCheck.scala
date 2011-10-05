package logicops.mailproc

import akka.actor.Actor
import collection.mutable
import akka.event.EventHandler
import logicops.db._

class UserCheck extends Actor {

  val privileged_user_emails = new mutable.HashMap[String, Node] with mutable.SynchronizedMap[String, Node]
  val client_user_emails = new mutable.HashMap[String, Node] with mutable.SynchronizedMap[String, Node]

  private def doLoad() {
    EventHandler.info(this, "Reloading privileged user email cache")
    val start = System.currentTimeMillis()

    val lw_account = Node.find("Account", "Name" -> "Logicworks").head
    EventHandler.debug(this, "Found Logicworks account: %s".format(lw_account))
    val privileged_users = lw_account.connectors.having("Child", "User")
    val accounts_container = Node.find("Generic Container Node", "Name" -> "Accounts").head
    val accounts = accounts_container.connectors.having("Child", "Account")
    val client_users = accounts.connectors.having("Child", "User")

    privileged_user_emails.empty
    for ((node_id : Int, user : Node) <- privileged_users) {
      user.attr("Email Address") match {
        case Some(na) => privileged_user_emails(na.value.trim.toLowerCase) = user
        case None =>
      }
      user.attr("Alternate Email Address") match {
        case Some(na) => privileged_user_emails(na.value.trim.toLowerCase) = user
        case None =>
      }
    }

    client_user_emails.empty
    for ((node_id : Int, user : Node) <- client_users) {
      user.attr("Email Address") match {
        case Some(na) => client_user_emails(na.value.trim.toLowerCase) = user
        case None =>
      }
      user.attr("Alternate Email Address") match {
        case Some(na) => client_user_emails(na.value.trim.toLowerCase) = user
        case None =>
      }
    }
    val end = System.currentTimeMillis()
    EventHandler.info(
      this,
      "Finished reloading user email cache ( %d ms )\n\tPrivileged user count: %s\n\tClient user count: %s".format(
        (end - start), privileged_user_emails.size, client_user_emails.size
      )
    )
  }

  override def preStart() {
    EventHandler.debug(this, "In preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
    doLoad()
  }

  override def preRestart(reason : Throwable) {
    EventHandler.debug(this, "In preRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.debug(this, "In postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  def receive = {
    case GetUserType(addr) => {
      val raw_address = EmailParser.prettyAddress(addr)
      if (privileged_user_emails.contains(raw_address)) {
        self reply LwPrivilegedUser(raw_address, privileged_user_emails(raw_address))
      } else if (client_user_emails.contains(raw_address)) {
        self reply LwClientUser(raw_address, client_user_emails(raw_address))
      } else {
        self reply LwUnknownUser(raw_address)
      }
    }

    case Reload() => {
      doLoad()
    }
    case _ => EventHandler.error(this, "Unknown message sent to UserCheck actor")
  }
}