import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class AttributeOptionTest extends Specification {

  "attribute_option with attribute_option_id 1225" should {
    "have value of 'Resolved'" in new ao {
      ao2.value must_== "Resolved"
    }
    "have attribute name of 'Service Request Sub-Status'" in new ao {
      ao2.attribute.name must_== "Service Request Sub-Status"
    }
  }

  "creating a new AttributeOption" should {
    "succeed" in new ao {
      AttributeOption.get(ao1.id.get).value must_== "TestArea"
    }
    "able to be updated" in new ao {
      ao1.copy(value = "TestArea2").save().get.value must_== "TestArea2"
    }
    "able to be deleted" in new ao {
      ao1.delete()
      AttributeOption.getOption(ao1.id.get) must_== None
    }
  }
  trait ao extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }
    val ao1 = AttributeOption(Attribute.get("Area"), "TestArea").save().get
    val ao2 = AttributeOption.get(1225)
  }
}