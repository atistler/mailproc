import org.specs2.mutable._
import logicops.db._

class NodeTypeTest extends Specification {

  val node_types = NodeType.allByPoolId(1008)
  "NodeTypes with node_type_pool_id of 1008" should {
    "have length 11" in {
      node_types.length must_== 20
    }
  }
  val node_type = NodeType.getMem(1057)
  "NodeType with node_type_id of 1057" should {
    "have name 'Service Request" in {
      node_type.name must_== "Service Request"
    }
  }

}