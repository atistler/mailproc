import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class TemplateAttributeTest extends Specification {

  val template_attribute = TemplateAttribute.getMem(1001)

  "The template_attribute with id 1001" should {
    "have template_id of 1" in {
      template_attribute.template.id.get must_== 1
    }
    "have a value of 'Root Node'" in {
      template_attribute.value must_== "Root Node"
    }
  }
}