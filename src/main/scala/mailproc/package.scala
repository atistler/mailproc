package object mailproc {

  import java.io.File
  import javax.mail.Address
  import logicops.db._
  import akka.actor.Actor._

  sealed trait MpMessage

  case class GetUserType(address : Address) extends MpMessage

  case class LwPrivilegedUser(address : String, user : Node) extends MpMessage

  case class LwClientUser(address : String, user : Node) extends MpMessage

  case class LwUnknownUser(address : String) extends MpMessage

  case class Reload() extends MpMessage

  case class ReloadComplete() extends MpMessage

  case class StartWatch() extends MpMessage

  case class StopWatch() extends MpMessage

  case class EmailFile(file : File) extends MpMessage

  case class AddOutgoingEmail(user: Node, srNodeId: Int, subject: String, to: String, from: String, body: String, file: File)

  case class AddIncomingEmail(user: Node, srNodeId: Int, subject: String, to: String, from: String, body: String, file: File)

  case class CreateNewSR(user: Node, subject: String, to: String, from: String, body: String, file : File)

  private[mailproc] def timed(blockName : String)(f : => Any) = {
    val start = System.currentTimeMillis
    f
    () => blockName + " took " + (System.currentTimeMillis - start) + "ms."
  }

  private[mailproc] def findConfig() = {
    val props_path = sys.props.get("mailproc.properties") getOrElse (sys.env.get(
      "mailproc.properties"
    ) getOrElse ("/mailproc.properties"))
    val props = new java.util.Properties()
    try {
      props.load(getClass.getResourceAsStream(props_path))
    } catch {
      case e : Exception => {
        sys.error("Could not load properties file: " + props_path)
        sys.exit(1)
      }
    }
    props
  }

  private[mailproc] val PROPS = findConfig()
  private[mailproc] val maildir_directory = PROPS.getProperty("maildir.directory")
  private[mailproc] val support_addresses = PROPS.getProperty("support-addresses").split(",").map(_.trim()).toSet
  private[mailproc] val userCheck = actorOf(new UserCheck)
  private[mailproc] val emailParser = actorOf(new EmailParser(support_addresses))
  private[mailproc] val directoryWatcher = actorOf(new DirectoryWatcher(maildir_directory))
  private[mailproc] val ticketHandler = actorOf(new TicketHandler)

}

