import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class NodeTest extends Specification {

  trait n1 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }

    val n1 = Node.get(527321)
  }

  "The node with id 527321" should {
    "have node_id of 527321" in new n1 {
      n1.id must_== Some(527321)
    }
    "have name = 'Root User'" in new n1 {
      n1.attr("Name").get.value must_== "SR 2-527321"
    }
    "have Service Request Status = 'Closed'" in new n1 {
      n1.attr("Service Request Status").get.value must_== "Closed"
    }
    "have 17 connectors" in new n1 {
      n1.connectors().size must_== 17
    }
    "have 15 connectees" in new n1 {
      n1.connectees().size must_== 15
    }
    "have 10 child connectees" in new n1 {
      n1.connectees.having(connectionTypes = List(ConnectionType.get("Parent"))).size must_== 10
    }
    "have 5 parent connectees" in new n1 {
      n1.connectees.having(connectionTypes = List(ConnectionType.get("Child"))).size must_== 5
    }
    "have 10 child connectors" in new n1 {
      n1.connectors.having(connectionTypes = List(ConnectionType.get("Child"))).size must_== 10
    }
    "have 5 parent connectors" in new n1 {
      n1.connectors.having(connectionTypes = List(ConnectionType.get("Parent"))).size must_== 5
    }
    "have 10 children of node type Activity" in new n1 {
      n1.connectors.having(
        connectionTypes = List(ConnectionType.get("Child")), nodeTypes = List(NodeType.get("Activity"))
      ).size must_== 10
    }
    "have 0 children of node type Asset" in new n1 {
      n1.connectors.having(
        connectionTypes = List(ConnectionType.get("Child")), nodeTypes = List(NodeType.get("Asset"))
      ).size must_== 0
    }
  }

  trait n2 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }

    val n2 = Node.getAll(119864, 119866)
  }

  "The nodes with ids 119864,119866" should {
    "have length of 2" in new n2 {
      n2.values.size must_== 2
    }
    "when updating Name to 2.0.0.0, name must equal 2.0.0.0" in new n2 {
      n2(119864).setAttr("Name", "2.0.0.0").attr("Name").get.value must_== "2.0.0.0"
    }
    "able to select children" in new n2 {
      val n3 = n2.connectors.having("Parent")
      n3.size must_== 2
    }
  }

  "Creating new node" should {
    "Have correct node_type" in {
      val node = Node.createFrom("Generic Service Request")
      node.nodeType.name must_== "Service Request"
    }
  }

  "Finding node 'Accounts'" should {
    "Have name 'Accounts'" in {
      val n = Node.find("Generic Container Node", "Name" -> "Accounts")
      n.head.valueOf("Name").get must_== "Accounts"
    }
  }

  "Finding LW SRQ" should {
    "Have name Service Request Queue" in {
      val n = Node.find("Account", "Name" -> "Logicworks").head
      val srqs = n.connectors.having("Child", "Service Request Queue")
      srqs.head._2.valueOf("Name").get must_== "Service Request Queue"
    }
  }


}
