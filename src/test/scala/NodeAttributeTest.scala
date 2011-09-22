import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class NodeAttributeTest extends Specification {

  "The node_attribute with id 1006" should {
    "have node_id of 1001" in {
      val node_attribute = NodeAttribute.get(1006)
      node_attribute.nodeId must_== 1001
    }
    "have a value of 'Physical'" in {
      val node_attribute = NodeAttribute.get(1006)
      node_attribute.value must_== "Physical"
    }
  }

  "Newly created NodeAttribute" should {
    "have value of 'Test Area'" in new na1 {
      na.value must_== "Test Area"
    }
    "able to be updated" in new na1 {
      na.copy(value = "Test Area2").save().get.value must_== "Test Area2"
    }
    "able to be deleted" in new na1 {
      na.delete()
      NodeAttribute.getOption(na.id.get) must_== None
    }
  }
  trait na1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }
    val na = NodeAttribute(Node.get(119840), "Area", "Test Area").save().get
  }

}