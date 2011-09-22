import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class AttributeOptionTest extends Specification {

  "attribute_option with attribute_option_id 1225" should {
    "have value of 'Resolved'" in {
      val attribute_option = AttributeOption.get(1225)
      attribute_option.value must_== "Resolved"
    }
    "have attribute name of 'Service Request Sub-Status'" in {
      val attribute_option = AttributeOption.get(1225)
      attribute_option.attribute.name must_== "Service Request Sub-Status"
    }
  }

  "creating a new AttributeOption" should {
    "succeed" in new ao1 {
      AttributeOption.get(ao.id.get).value must_== "TestArea"
    }
    "able to be updated" in new ao1 {
      ao.copy(value = "TestArea2").save().get.value must_== "TestArea2"
    }
    "able to be deleted" in new ao1 {
      ao.delete()
      AttributeOption.getOption(ao.id.get) must_== None
    }
  }
  trait ao1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }
    val ao = AttributeOption(Attribute.get("Area"), "TestArea").save().get
  }
}