package db {

import org.orbroker._
import scala.collection.mutable.HashMap

class NodeSeq(nseq : IndexedSeq[Node]) {
  def nodes() {
    nseq foreach {
      n => println(n)
    }
  }
}

class Node(var id : Option[Int], var nodeTypeId : Int, var templateId : Int) extends Dao {
  private[db] val attributes = new HashMap[String, NodeAttribute]()

  override def toString = "Node[%s] (nodeType: %s[%d], template: %s[%d])".format(
    id, nodeType.name, nodeTypeId, template, templateId
  )

  object connectors {
    def apply() = {
      val c = broker.readOnly() {
        s => s.selectAll(Tokens.Node.selectByConnectee, "connecteeId" -> id)
      }
      c
    }
  }

  object connectees {
    def apply() = {
      val c = broker.readOnly() {
        s => s.selectAll(Tokens.Node.selectByConnector, "connectorId" -> id)
      }
      c
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
      broker.readOnly() {
        s => s.selectAll(
          Tokens.Node.selectByConnector, "connectorId" -> id,
          "connectionTypeIds" -> mapToIds(connectionTypes), "connectorTypeIds" -> mapToIds(connectorTypes),
          "connecteeTypeIds" -> mapToIds(connecteeTypes)
        )
      }
    }
  }

  /*
  def connectees() = {
    broker.readOnly() {
      s => s.selectAll(Tokens.Connection.selectByConnectorNodeId, "connector_node_id" -> id)
    }
  }
  */
  /*
  def connectors() = {
    broker.readOnly() {
      s => s.selectAll(Tokens.Connection.selectByConnectee, "connectee_node_id" -> id)
    }
  }
  */

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

  def setAttr(attributeName : String, value : String) = {
    attr(attributeName) match {
      case Some(na) => {
        broker.transaction() {
          _.callForUpdate(
            Tokens.NodeAttribute.update, "node_attribute_map_id" -> na.id.get, "attribute_id" -> na.attributeId,
            "value" -> value
          )
        }
        na.value = value
        this
      }

      case None => {
        broker.transaction() {
          _.callForKeys(
            Tokens.NodeAttribute.insert, "attribute_id" -> Attribute.get(attributeName).id.get, "value" -> value
          ) {
            k : Int => {
              val na = NodeAttribute(k, this, Attribute.get(attributeName), value)
              attributes += attributeName -> na
            }
          }
        }
        this
      }
    }
  }

  def save() = {
    broker.transaction() {
      t =>
        id match {
          case Some(_id) => throw new DaoException("Nodes cannot be updated: " + this)
          case None => t.callForKeys(
            Tokens.Node.insert, "node_type_id" -> nodeType.id.get, "template_id" -> template.id.get
          ) {
            k : Int => id = Some(k)
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

  def apply(id : Int, nodeType : NodeType, templateId : Int) : Node = {
    apply(id, nodeType.id.get, templateId)
  }

  def apply(id : Int, nodeTypeId : Int, template : Template) : Node = {
    apply(id, nodeTypeId, template.id.get)
  }

  def apply(id : Int, nodeType : NodeType, template : Template) : Node = {
    apply(id, nodeType.id.get, template.id.get)
  }

  def apply(nodeTypeId : Int, templateId : Int) : Node = {
    new Node(None, nodeTypeId, templateId)
  }

  def apply(nodeType : NodeType, templateId : Int) : Node = {
    apply(nodeType.id.get, templateId);
  }

  def apply(nodeTypeId : Int, template : Template) : Node = {
    apply(nodeTypeId, template.id.get)
  }

  def apply(nodeType : NodeType, template : Template) : Node = {
    apply(nodeType.id.get, template.id.get)
  }

  def getOption(id : Int) = {
    selectOneOption(Tokens.Node.selectById, "node_id" -> id)
  }

  def getAll(ids : Int*) = {
    selectAllOption(Tokens.Node.selectByIds, "nodeIds" -> ids.toArray)
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