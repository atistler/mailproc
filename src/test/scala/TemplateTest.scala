import logicops.util.RollbackSpec
import org.specs2.mutable._
import logicops.db._

class TemplateTest extends Specification with RollbackSpec {

  "The template with id 1" should {
    val t1 = Template.get(1)
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

  "Templates with node_type_ids of '1005, 1009'" should {
    val t2 = Template.all(nodeTypes = List(NodeType.get(1005), NodeType.get(1009)))
    "number of templates should be 12" in {
      t2.length must_== 12
    }
  }

  "Templates with node_type_ids of '1005, 1009' and attribute id of 1" should {
    val t3 = Template.all(
      nodeTypes = List(NodeType.get(1005), NodeType.get(1009)), attributes = List(Attribute.get(1))
    )
    "number of templates should be 12" in {
      t3.length must_== 12
    }
  }
}