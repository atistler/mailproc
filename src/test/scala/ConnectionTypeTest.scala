import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class ConnectionTypeTest extends Specification {

  "creating a new ConnectionType" should {
    "succeed" in new c1 {
      ct.name must_== ConnectionType.get(ct.id.get).name
    }
    "able to be updated" in new c1 {
      ct.copy(name = "Test Conn Type2").save().name must_== "Test Conn Type2"
    }
    "able to be deleted" in new c1 {
      ct.delete()
      ConnectionType.getOption(ct.id.get) must_== None
    }
  }

  trait c1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }

    val ct = ConnectionType("Test Conn Type", false, 0).save().get
  }

}