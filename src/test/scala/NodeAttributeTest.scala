import org.specs2.mutable._
import logicops.db._

class NodeAttributeTest extends Specification {

  val node_attribute = NodeAttribute.getMem(1006)
  "The node_attribute with id 1006" should {
    "have node_id of 1001" in {
      node_attribute.nodeId must_== 1001
    }
    "have a value of 'Physical'" in {
      node_attribute.value must_== "Physical"
    }
  }
}