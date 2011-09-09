import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class AttributeTest extends Specification  {

  "attribute with attribute_id 1" should {
    "have interface_name of 'Name'" in {
      val a = Attribute.getMem(1)
      a.name must_== "Name"
    }
  }

  "attribute with attribute_id 1072" should {
    "have interface_name of 'Service Request Sub-Status'" in {
      val a = Attribute.getMem(1072)
      a.name must_== "Service Request Sub-Status"
    }
    "have 9 attribute options" in {
      val a = Attribute.getMem(1072)
      a.options.length must_== 9
    }
  }

  "attribute with name 'Test Attribute'" should {
    "have interface_name of 'Test Attribute" in {
      val a = Attribute("Test Attribute", false).save()
      Attribute.get(a.name).name must_== "Test Attribute"
    }
  }

  "saved attribute's name" should {
    "have interface_name of 'Test Attr2'" in new a1 {
      a.name must_== "Test Attr2"
    }
    "deleting 'Test Attr2" in new a1 {
      Attribute.get(a.name).delete()
      Attribute.getOption(a.name) must_== None
    }
  }

  trait a1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }

    val a = Attribute("Test Attr2", false).save()
  }

}