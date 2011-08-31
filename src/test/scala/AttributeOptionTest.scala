import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class AttributeOptionTest extends Specification {

  val attribute_option = AttributeOption.getMem(1225)
  "attribute_option with attribute_option_id 1225" should {
    "have value of 'Resolved'" in {
      attribute_option.value must_== "Resolved"
    }
    "have attribute name of 'Service Request Sub-Status'" in {
      attribute_option.attribute.name must_== "Service Request Sub-Status"
    }
  }
}