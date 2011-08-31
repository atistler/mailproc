import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class TemplateTest extends Specification {

  val t1 = Template.getMem(1)

  "The template with id 1" should {
    "have template_id of 1" in {
      t1.id must_== Some(1)
    }
    "have name = 'Root Node'" in {
      t1.attr("Name").get.value must_== "Root Node"
    }
    "have node_type_id of 1" in {
      t1.nodeType.id.get must_== 1
    }
  }

  val t2 = Template.all(nodeTypes = List(NodeType.getMem(1005), NodeType.getMem(1009)))
  "Templates with node_type_ids of '1005, 1009'" should {
    "number of templates should be 12" in {
      t2.length must_== 12
    }
  }
  val t3 = Template.all(nodeTypes = List(NodeType.getMem(1005), NodeType.getMem(1009)), attributes = List(Attribute.getMem(1)))
  "Templates with node_type_ids of '1005, 1009' and attribute id of 1" should {
    "number of templates should be 12" in {
      t3.length must_== 12
    }
  }
}