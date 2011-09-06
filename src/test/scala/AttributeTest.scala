import logicops.util.RollbackSpec
import org.specs2.mutable._
import logicops.db._

class AttributeTest extends Specification with RollbackSpec {


  "attribute with attribute_id 1" should  {
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
    "have interface_name of 'Test Attr2'" in {
      val a = Attribute("Test Attr1", false).save().copy(name = "Test Attr2").save()
      a.name must_== "Test Attr2"
    }
    "deleting 'Test Attr3" in {
      val a = Attribute("Test Attr3", false).save()
      Attribute.get(a.name).delete()
      Attribute.getOption(a.name) must_== None
    }
  }
}