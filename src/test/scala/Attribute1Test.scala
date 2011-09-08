import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class Attribute1Test extends Specification {

  "saved attribute's name" should {
    "have interface_name of 'Test Attr2'" in new a1 {
      broker.transactional(connection) {
        t =>
          val a = Attribute("Test Attr2", false).save()
          a.name must_== "Test Attr2"
      }
    }
    "deleting 'Test Attr2" in new a1 {
      broker.transactional(connection) {
        t =>
          Attribute.get(a.name).delete()
          Attribute.getOption(a.name) must_== None
      }
    }
  }

  trait a1 extends Scope with After {
    def after {
      connection.rollback()
    }


  }

}