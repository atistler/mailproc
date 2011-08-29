import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeTest extends Specification {

  val node = Node.get(527321)

  "The node with id 527321" should {
    "have node_id of 527321" in {
      node.id must_== Some(527321)
    }
    "have name = 'Root User'" in {
      node.getAttr("Name").get must_== "SR 2-527321"
    }
    "have Service Request Status = 'Closed'" in {
      node.getAttr("Service Request Status").get must_== "Closed"
    }
    /*
    "have 17 connectors" in {
      node.connectors().length must_== 17
    }
    "have 15 connectees" in {
      node.connectees().length must_== 15
    }
    */
  }
}