package db {

import org.orbroker._
import scala.collection.mutable

class NodeSeq(nseq : IndexedSeq[Node]) {
  def nodes() {
    nseq foreach {
      n => println(n)
    }
  }
}

class Node(val id : Option[Int], val nodeTypeId : Int, val templateId : Int) extends Dao {
  private[db] val attributes = mutable.Map.empty[String, NodeAttribute]

  private val self = this
  override def toString = "Node[%s] (nodeType: %s[%d], template: %s[%d])".format(
    id, nodeType.name, nodeTypeId, template.name, templateId
  )

  def copy(id : Option[Int] = this.id, nodeTypeId : Int = this.nodeTypeId, templateId : Int = this.templateId) = {
    new Node(id, nodeTypeId, templateId).attributes ++= this.attributes
  }

  object connectors {
    def apply() = {
      broker.transactional(connection) {
        println(self)
        s => s.selectAll(Tokens.Node.selectByConnectee, "node" -> self)
      }
    }
    def having(
      connectionTypes : List[ConnectionType] = Nil, connectorTypes : List[NodeType] = Nil,
      connecteeTypes : List[NodeType] = Nil
      ) = {
      broker.transactional(connection) {
        s => s.selectAll(
          Tokens.Node.selectByConnectee, "node" -> self,
          "connectionTypes" -> mapToIds(connectionTypes).toArray, "connectorTypes" -> mapToIds(connectorTypes).toArray,
          "connecteeTypes" -> mapToIds(connecteeTypes).toArray
        )
      }
    }
  }

  object connectees {
    def apply() = {
      broker.transactional(connection) {
        println(self)
        s => s.selectAll(Tokens.Node.selectByConnector, "node" -> self)
      }
    }

    /*
    def having(connectionType: Option[ConnectionType] = None, connectorType: Option[NodeType] = None, connecteeType: Option[NodeType] = None) : IndexedSeq[Node] = {
      val connectionTypes = connectionType.map(List(_)) getOrElse Nil
      val connectorTypes = connectorType.map(List(_)) getOrElse Nil
      val connecteeTypes = connecteeType.map(List(_)) getOrElse Nil
      having(connectionTypes, connectorTypes, connecteeTypes)
    }
    */
    def having(
      connectionTypes : List[ConnectionType] = Nil, connectorTypes : List[NodeType] = Nil,
      connecteeTypes : List[NodeType] = Nil
      ) = {
      broker.transactional(connection) {
        s => s.selectAll(
          Tokens.Node.selectByConnector, "node" -> self,
          "connectionTypes" -> mapToIds(connectionTypes).toArray, "connectorTypes" -> mapToIds(connectorTypes).toArray,
          "connecteeTypes" -> mapToIds(connecteeTypes).toArray
        )
      }
    }
  }

  lazy val nodeType = NodeType.getMem(nodeTypeId)
  lazy val template = Template.getMem(templateId)

  def attr(s : String) = {
    attributes.get(s)
  }

  def valueOf(s : String) = {
    attr(s) match {
      case Some(na) => Some(na.value)
      case None => None
    }
  }

  def addConnection(connectionType : ConnectionType, node: Node) = {
    val c = Connection(connectionType, this, node)
    c
  }

  def setAttr(attribute: Attribute, value : String) = {
    setAttr(attribute.name, value)
  }

  def setAttr(attributeName : String, value : String) = {
    attr(attributeName) match {
      case Some(na) => {
        val newNa = na.copy(value = value)
        broker.transactional(connection) {
          _.execute(
            Tokens.NodeAttribute.update, "nodeAttribute" -> newNa
          )
        }
        attributes += attributeName -> newNa
        this
      }

      case None => {
        val na = NodeAttribute(this, Attribute.get(attributeName), value)
        val key = broker.transactional(connection) {
          _.executeForKey(
            Tokens.NodeAttribute.insert, "attribute_id" -> Attribute.get(attributeName).id.get, "value" -> value
          )
        }
        attributes += attributeName -> na.copy(id = key)
        this
      }
    }
  }

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => throw new DaoException("Nodes cannot be updated: " + this)
          case None => t.executeForKeys(
            Tokens.Node.insert, "node" -> this
          ) {
            k : Int => copy(id = Some(k))
          }
        }
    }
  }

  /* TODO:
   def setAttr(attribute: Attribute, value: String): Node
   def setAttr(attributeId: Int, value: String): Node
   def setAttrs(Map[Attribute, value]): Node
   def connectors(node_type: NodeType*) : IndexedSeq[Node]
   def connectors(connection_type: ConnectionType*) : IndexedSeq[Node]
   def connectors(attribute: Attribute*) : IndexedSeq[Node]
   def connectors(attribute_values: Map[Attribute, String]) : IndexedSeq[Node]

   def connectors(node_type: NodeType*, connection_type: ConnectionType*) : IndexedSeq[Node]
   def connectors(node_type: NodeType*, attribute: Attribute*) : IndexedSeq[Node]
   def connectors(connection: NodeType*, attribute: Attribute*) : IndexedSeq[Node]

   def connectors(node_type: NodeType*, connection_type: ConnectionType*, attribute: Attribute*) : IndexedSeq[Node]

   def connectees(node_type: NodeType*) : IndexedSeq[Node]
   def connectees(node_type: NodeType*, connection_type: ConnectionType*) : IndexedSeq[Node]
   def connectors(connection_type: ConnectionType*) : IndexedSeq[Node]
  */
}

object Node extends DaoHelper[Node] {

  def apply(id : Int, nodeTypeId : Int, templateId : Int) : Node = {
    new Node(Some(id), nodeTypeId, templateId)
  }

  def apply(id : Int, nodeType : NodeType, template : Template) : Node = {
    apply(id, nodeType.id.get, template.id.get)
  }

  def apply(id : Int, nodeType : String, template : String) : Node = {
    apply(id, NodeType.get(nodeType), Template.get(template))
  }

  def apply(nodeTypeId : Int, templateId : Int) : Node = {
    new Node(None, nodeTypeId, templateId)
  }

  def apply(nodeType : NodeType, template : Template) : Node = {
    apply(nodeType.id.get, template.id.get)
  }

  def apply(nodeType : String, template : String) : Node = {
    apply(NodeType.get(nodeType), Template.get(template))
  }

  def getOption(id : Int) = {
    selectOneOption(Tokens.Node.selectById, "node_id" -> id)
  }

  def getAll(ids : Int*) = {
    val nodes = selectAllOption(Tokens.Node.selectByIds, "nodeIds" -> ids.toArray)
    nodes.foldLeft(mutable.Map.empty[Int,Node]) { (m,n) => m(n.id.get) = n; m }
  }
}

object NodeExtractor extends JoinExtractor[Node] {
  val key = Set("node_id")

  def extract(row : Row, join : Join) = {
    val node = Node(
      row.integer("node_id").get,
      row.integer("node_type_id").get,
      row.integer("template_id").get
    )
    for (na <- join.extractSeq(NodeAttributeExtractor, Map.empty)) {
      node.attributes += na.attribute.name -> na
    }
    node
  }
}

/*
object NodesExtractor extends JoinExtractor[mutable.Map[Int,Node]] {

  def extract(row: Row, join: Join) = {
    val nodes = mutable.Map.empty[Int, Node]
    for ( node <- join.extractGroup(NodeExtractor, Map.empty)) {
      nodes += node.id.get -> node
    }
    nodes
  }
}
*/

}