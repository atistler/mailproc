import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeTypeTest extends Specification {

  val node_types = NodeType.allByPoolId(1008)
  "NodeTypes with node_type_pool_id of 1008" should {
    "have length 11" in {
      node_types.length must_== 20
    }
  }
  val node_type = NodeType.get(1057)
  "NodeType with node_type_id of 1057" should {
    "have name 'Service Request" in {
      node_type.name must_== "Service Request"
    }
  }

}