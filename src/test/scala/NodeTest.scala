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

  node.connectees().nodes

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
    "have 17 connectors" in {
      node.connectors().length must_== 17
    }
    "have 15 connectees" in {
      node.connectees().length must_== 15
    }
  }

  val template = Template.get(1)
  "The template with id 1" should {
    "have template_id of 1" in {
      template.id must_== Some(1)
    }
    "have name = 'Root Node'" in {
      template.getAttr("Name").get must_== "Root Node"
    }
    "have node_type_id of 1" in {
      template.nodeType.id.get must_== 1
    }
  }

  val template_attribute = TemplateAttribute.get(1001)
  "The template_attribute with id 1001" should {
    "have template_id of 1" in {
      template_attribute.template.id.get must_== 1
    }
    "have a value of 'Root Node'" in {
      template_attribute.value must_== "Root Node"
    }
  }

  val templates = Template.all(nodeTypes=List(NodeType.get(3), NodeType.get(1047)))
  "templates with node_type_id 3" should {
    "number of templates should be 10" in {
      templates.length must_== 10
    }
  }

  val attribute = Attribute.get(1)
  "attribute with attribute_id 1" should  {
    "have interface_name of Name" in {
      attribute.name must_== "Name"
    }
  }

  val node_types = NodeType.allByPoolId(1008)
  "NodeTypes with node_type_pool_id of 1008" should {
    "have length 11" in {
      node_types.length must_== 20
    }
  }

}