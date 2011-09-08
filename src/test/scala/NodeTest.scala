import logicops.util.RollbackSpec
import org.specs2.mutable._
import logicops.db._

class NodeTest extends Specification with RollbackSpec {

  "The node with id 527321" should {
    val n1 = Node.getMem(527321)
    "have node_id of 527321" in {
      n1.id must_== Some(527321)
    }
    "have name = 'Root User'" in {
      n1.attr("Name").get.value must_== "SR 2-527321"
    }
    "have Service Request Status = 'Closed'" in {
      n1.attr("Service Request Status").get.value must_== "Closed"
    }
    "have 17 connectors" in {
      n1.connectors().length must_== 17
    }
    "have 15 connectees" in {
      n1.connectees().length must_== 15
    }
    "have 10 child connectees" in {
      n1.connectees.having(connectionTypes = List(ConnectionType.get("Parent"))).length must_== 10
    }
    "have 5 parent connectees" in {
      n1.connectees.having(connectionTypes = List(ConnectionType.get("Child"))).length must_== 5
    }
    "have 10 child connectors" in {
      n1.connectors.having(connectionTypes = List(ConnectionType.get("Child"))).length must_== 10
    }
    "have 5 parent connectors" in {
      n1.connectors.having(connectionTypes = List(ConnectionType.get("Parent"))).length must_== 5
    }
    "have 10 children of node type Activity" in {
      n1.connectors.having(
        connectionTypes = List(ConnectionType.get("Child")), nodeTypes = List(NodeType.get("Activity"))
      ).length must_== 10
    }
    "have 0 children of node type Asset" in {
      n1.connectors.having(
        connectionTypes = List(ConnectionType.get("Child")), nodeTypes = List(NodeType.get("Asset"))
      ).length must_== 0
    }
  }

  "The nodes with ids 119864,119866" should {
    val n2 = Node.getAll(119864, 119866)
    "have length of 2" in {
      n2.values.size must_== 2
    }
    "when updating Name to 2.0.0.0, name must equal 2.0.0.0" in {
      n2(119864).setAttr("Name", "2.0.0.0").attr("Name").get.value must_== "2.0.0.0"
    }
  }

  "Creating new node" should {
    val node = Node.createFrom("Generic Service Request")
    "Have correct node_type" in {
      node.nodeType.name must_== "Service Request"
    }
  }
}
