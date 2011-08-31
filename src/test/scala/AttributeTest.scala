import org.specs2.mutable._
import db._

/**
 * Created by IntelliJ IDEA.
 * User: atistler
 * Date: 8/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */

class AttributeTest extends Specification {

  val a1 = Attribute.getMem(1)
  val a3 = Attribute.getMem(1)
  val a4 = Attribute.getMem(1)
  val a5 = Attribute.getMem(1)
  val a6 = Attribute.getMem(1)
  val a7 = Attribute.getMem(1)
  val a8 = Attribute.getMem(1)
  val a9 = Attribute.getMem(1)
  "attribute with attribute_id 1" should  {
    "have interface_name of 'Name'" in {
      a1.name must_== "Name"
    }
  }
  val a2 = Attribute.getMem(1072)
  "attribute with attribute_id 1072" should {
    "have interface_name of 'Service Request Sub-Status'" in {
      a2.name must_== "Service Request Sub-Status"
    }
    "have 9 attribute options" in {
      a2.options.length must_== 9
    }
  }
}