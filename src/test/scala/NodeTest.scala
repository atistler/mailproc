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
  val n1 = Node.getMem(527321)
  "The node with id 527321" should {
    "have node_id of 527321" in {
      n1.id must_== Some(527321)
    }
    "have name = 'Root User'" in {
      n1.attr("Name").get.value must_== "SR 2-527321"
    }
    "have Service Request Status = 'Closed'" in {
      n1.attr("Service Request Status").get.value must_== "Closed"
    }
    "have 17 connectors" in {
      n1.connectors().length must_== 17
    }
    "have 15 connectees" in {
      n1.connectees().length must_== 15
    }
  }
  val n2 = Node.getAll(119864,119866)
  "The nodes with ids 119864,119866" should {
    "have length of 2" in {
      n2.length must_== 2
    }
    "have first node name equal '10.4.23.149'" in {
      n2(0).attr("Name").get.value must_== "10.4.23.149"
    }
    "have second node name equal '10.4.23.151'" in {
      n2(1).valueOf("Name").get must_== "10.4.23.151"
    }
  }
}