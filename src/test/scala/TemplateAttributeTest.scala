import logicops.util.RollbackSpec
import org.specs2.mutable._
import logicops.db._

class TemplateAttributeTest extends Specification with RollbackSpec {

  "The template_attribute with id 1001" should {
  val template_attribute = TemplateAttribute.getMem(1001)
    "have template_id of 1" in {
      template_attribute.template.id.get must_== 1
    }
    "have a value of 'Root Node'" in {
      template_attribute.value must_== "Root Node"
    }
  }
}