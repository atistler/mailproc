import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class NodeTypeTest extends Specification {

  "NodeTypes with node_type_pool_id of 1008" should {
    "have length 11" in {
      val node_types = NodeType.allByPoolId(1008)
      node_types.length must_== 20
    }
  }

  "NodeType with node_type_id of 1057" should {
    "have name 'Service Request" in {
      val node_type = NodeType.get(1057)
      node_type.name must_== "Service Request"
    }
  }

  "Creating a new NodeType" should {
    "have a interface_name of 'Test Node Type'" in new nt1 {
      NodeType.get(nt.id.get).name must_== "Test Node Type"
    }
    "have a poolid of 1499" in new nt1 {
      NodeType.get(nt.id.get).poolId must_== 1499
    }
    "able to be updated" in new nt1 {
      nt.copy(name = "Test Node Type2").save()
      NodeType.get(nt.id.get).name must_== "Test Node Type2"
    }
    "able to be deleted" in new nt1 {
      nt.delete()
      NodeType.getOption(nt.id.get) must_== None
    }
  }

  trait nt1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }
    val nt = NodeType(1499, "Test Node Type").save().get
  }


}