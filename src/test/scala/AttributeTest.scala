import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class AttributeTest extends Specification  {

  "attribute with attribute_id 1" should {
    "have interface_name of 'Name'" in new a {
      a4.name must_== "Name"
    }
  }

  "attribute with attribute_id 1072" should {
    "have interface_name of 'Service Request Sub-Status'" in new a{
      a3.name must_== "Service Request Sub-Status"
    }
    "have 9 attribute options" in new a{
      a3.options.length must_== 9
    }
  }

  "attribute with name 'Test Attribute'" should {
    "have interface_name of 'Test Attribute" in new a {
      Attribute.get(a2.name).name must_== "Test Attribute"
    }
  }

  "saved attribute's name" should {
    "have interface_name of 'Test Attr2'" in new a {
      a1.name must_== "Test Attr2"
    }
    "deleting 'Test Attr2" in new a {
      Attribute.get(a1.name).delete()
      Attribute.getOption(a1.name) must_== None
    }
  }

  trait a extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }

    val a1 = Attribute("Test Attr2", false).save().get
    val a2 = Attribute("Test Attribute", false).save().get
    val a3 = Attribute.get(1072)
    val a4 = Attribute.get(1)
  }

}