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

  val t1 = Template.get(1)
  "The template with id 1" should {
    "have template_id of 1" in {
      t1.id must_== Some(1)
    }
    "have name = 'Root Node'" in {
      t1.getAttr("Name").get must_== "Root Node"
    }
    "have node_type_id of 1" in {
      t1.nodeType.id.get must_== 1
    }
  }

  val t2 = Template.all(NodeType.get(1005), NodeType.get(1009))
  "Templates with node_type_ids of '1005, 1009'" should {
    "number of templates should be 12" in {
      t2.length must_== 12
    }
  }
}