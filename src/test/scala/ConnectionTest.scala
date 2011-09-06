import org.specs2.mutable._
import logicops.db._

class ConnectionTest extends Specification {
  val c = Connection.getMem(3684799)
  "connection with connection_id 3684799" should {
    "have connectionType of 'Created By'" in {
      c.connectionType.name must_== "Created By"
    }
    "have connectorType of 'User'" in {
      c.connectorType.name must_== "User"
    }
    "have connecteeType of 'Activity'" in {
      c.connecteeType.name must_== "Activity"
    }
    "have connector Name of 'Lin, Xiao Fen '" in {
      c.connector.attr("Name").get.value must_== "Lin, Xiao Fen "
    }
    "have connectee Name of 'Activity 2-721462'" in {
      c.connectee.attr("Name").get.value must_== "Activity 2-721462"
    }
  }
  c.find() match {
    case Some(conn) => println(conn)
    case None => println("not found")
  }
}