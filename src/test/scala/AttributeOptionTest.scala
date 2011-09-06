import logicops.util.RollbackSpec
import org.specs2.mutable._
import logicops.db._

class AttributeOptionTest extends Specification with RollbackSpec {

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