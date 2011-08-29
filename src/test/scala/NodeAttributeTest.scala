import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeAttributeTest extends Specification {

  val node_attribute = NodeAttribute.get(1006)
  "The node_attribute with id 1006" should {
    "have node_id of 1001" in {
      node_attribute.nodeId must_== 1001
    }
    "have a value of 'Physical'" in {
      node_attribute.value must_== "Physical"
    }
  }
}