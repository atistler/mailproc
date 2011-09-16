import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class ConnectionTest extends Specification {

  "connection with connection_id 3684799" should {
    "have connectionType of 'Created By'" in new c1 {
      c.connectionType.name must_== "Created By"
    }
    "have connectorType of 'User'" in new c1 {
      c.connectorType.name must_== "User"
    }
    "have connecteeType of 'Activity'" in new c1 {
      c.connecteeType.name must_== "Activity"
    }
    "have connector Name of 'Lin, Xiao Fen '" in new c1 {
      c.connector.attr("Name").get.value must_== "Lin, Xiao Fen "
    }
    "have connectee Name of 'Activity 2-721462'" in new c1 {
      c.connectee.attr("Name").get.value must_== "Activity 2-721462"
    }
  }

  "creating a new Connection" should {
    "succeed" in new c2 {
      Connection.get(c.id.get).connecteeId must_== c.connecteeId
    }
    "able to be deleted" in new c2 {
      c.delete()
      Connection.getOption(c.id.get) must_== None
    }
  }

  trait c1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }
    val c = Connection.get(3684799)
  }

  trait c2 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }
    val c = Connection(ConnectionType.get("Parent"), Node.get(119840), Node.get(119841)).save()
  }

}