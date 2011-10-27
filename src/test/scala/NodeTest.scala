import org.specs2.mutable._
import logicops.db._
import org.specs2.specification.Scope

class NodeTest extends Specification {

  trait n1 extends Scope with After {
    def after {
      // Database.getConnection.rollback()
    }

    val n1 = Node.get(527321)
  }


  "The node with id 527321" should {
    "have node_id of 527321" in new n1 {
      n1.id must_== Some(527321)
    }
    "create connection to 983637" in new n1 {
      val n2 = Node.get(983637)
      n1.addParent(n2)
      Database.getConnection.commit()
    }
    "have name = 'SR 2-527321'" in new n1 {
      n1.attr("Name").get.value must_== "SR 2-527321"
    }
    "have Service Request Status = 'Closed'" in new n1 {
      n1.attr("Service Request Status").get.value must_== "Closed"
    }
    "have 18 connectors" in new n1 {
      n1.connectors().size must_== 18
    }
    "have 16 connectees" in new n1 {
      n1.connectees().size must_== 16
    }
    "have 10 child connectees" in new n1 {
      n1.connectees.having(connectionTypes = List(ConnectionType.get("Parent"))).size must_== 10
    }
    "have 6 parent connectees" in new n1 {
      n1.connectees.having("Child").size must_== 6
    }
    "have 10 child connectors" in new n1 {
      n1.connectors.having("Child").size must_== 10
    }
    "have 6 parent connectors" in new n1 {
      n1.connectors.having("Parent").size must_== 6
    }
    "have 10 children of node type Activity" in new n1 {
      n1.connectors.having(
        "Child", "Activity"
      ).size must_== 10
    }
    "have 0 children of node type Asset" in new n1 {
      n1.connectors.having(
        "Child", "Asset"
      ).size must_== 0
    }
    "Account Acadient must have an SRQ" in new n3 {
      val srq = n3.connectors.having("Child", "Name" -> "Service Request Queue")
      srq.head._2.valueOf("Name").get must_== "Service Request Queue"
    }
    "Account Acadient must have an Steve, Picardi" in new n3 {
      val u = n3.connectors.having("Child", "User", "First Name" -> "Steve", "Last Name" -> "Picardi")
      u.head._2.valueOf("Name").get must_== "Picardi, Steve"
    }
    "Account Acadient has 9 user and containers" in new n3 {
      val u1 = n3.connectors.having("Child", List(NodeType.get("User"), NodeType.get("Generic Container Node")))
      u1.size must_== 9
    }
    "Account2 Acadient has 9 user and containers" in new n3 {
      val u1 = n3.connectors.having("Child", List("User", "Generic Container Node"))
      u1.size must_== 9
    }
  }

  trait n3 extends Scope with After {
    def after {
      Database.getConnection.rollback()
    }

    val n3 = Node.find("Account", "Name" -> "Acadient").head
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
    "Have name Service Request Queue when using child()" in {
      val n = Node.find("Account", "Name" -> "Logicworks").head
      n.child("Service Request Queue").get._2.valueOf("Name").get must_== "Service Request Queue"
    }
  }


}
