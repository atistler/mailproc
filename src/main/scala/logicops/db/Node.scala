package logicops.db {

import org.orbroker._
import scala.collection.mutable.{Map => MuMap}

class Nodes(nodeMap : MuMap[Int, Node]) {

  object connectors {
    def apply() = {
      if (nodeMap.isEmpty) {
        nodeMap
      } else {
        broker.transactional(Database.getConnection) {
          _.selectAll(Node.Tokens.selectByConnectee, "nodes" -> nodeMap.keys.toArray)
        }.foldLeft(MuMap.empty[Int, Node]) {
          (m, n) => m(n.id.get) = n; m
        }
      }
    }

    def having(
      connectionTypes : Iterable[ConnectionType] = Nil, nodeTypes : Iterable[NodeType] = Nil
      ) = {
      if (nodeMap.isEmpty) {
        nodeMap
      } else {
        broker.transactional(Database.getConnection) {
          _.selectAll(
            Node.Tokens.selectByConnectee, "nodes" -> nodeMap.keys.toArray,
            "connectionTypes" -> mapToIds(connectionTypes), "connectorTypes" -> mapToIds(nodeTypes)
          )
        }.foldLeft(MuMap.empty[Int, Node]) {
          (m, n) => m(n.id.get) = n; m
        }
      }
    }
    def having(connectionType: String, nodeType: String) : MuMap[Int, Node] = {
      having(List(ConnectionType.get(connectionType)), List(NodeType.get(nodeType)))
    }
    def having(connectionType : String) : MuMap[Int, Node] = {
      having(List(ConnectionType.get(connectionType)), Nil)
    }
    def testhaving(connectionType : ConnectionType) : MuMap[Int, Node] = {
      if (nodeMap.isEmpty) {
        nodeMap
      } else {
        broker.transactional(Database.getConnection) {
          _.selectAll(
            Node.Tokens.selectByConnector, "nodes" -> nodeMap.keys.toArray,
            "connectionTypes" -> mapToIds(List(connectionType)), "connecteeTypes" -> mapToIds(Nil)
          )
        }.foldLeft(MuMap.empty[Int, Node]) {
          (m, n) => m(n.id.get) = n; m
        }
      }
    }
  }

  object connectees {
    def apply() = {
      if (nodeMap.isEmpty) {
        nodeMap
      } else {
        broker.transactional(Database.getConnection) {
          _.selectAll(Node.Tokens.selectByConnector, "nodes" -> nodeMap.keys.toArray)
        }.foldLeft(MuMap.empty[Int, Node]) {
          (m, n) => m(n.id.get) = n; m
        }
      }
    }

    def having(
      connectionTypes : Iterable[ConnectionType] = Nil, nodeTypes : Iterable[NodeType] = Nil
      ) = {
      if (nodeMap.isEmpty) {
        nodeMap
      } else {
        broker.transactional(Database.getConnection) {
          _.selectAll(
            Node.Tokens.selectByConnector, "nodes" -> nodeMap.keys.toArray,
            "connectionTypes" -> mapToIds(connectionTypes), "connecteeTypes" -> mapToIds(nodeTypes)
          )
        }.foldLeft(MuMap.empty[Int, Node]) {
          (m, n) => m(n.id.get) = n; m
        }
      }
    }
    def having(connectionType: String, nodeType: String) : MuMap[Int, Node] = {
      having(List(ConnectionType.get(connectionType)), List(NodeType.get(nodeType)))
    }
    def having(connectionType : String) : MuMap[Int, Node] = {
      having(List(ConnectionType.get(connectionType)), Nil)
    }

  }

}

/*
object Nodes {
  object Tokens extends TokenSet(true) {
    val selectByConnector = Token('selectNodesByConnector, NodeExtractor)
    val selectByConnectee = Token('selectNodesByConnectee, NodeExtractor)
  }
}
*/

class Node(val id : Option[Int], val nodeTypeId : Int, val templateId : Int) extends Dao {
  private[db] val attributes = MuMap.empty[String, NodeAttribute]

  private val self = this

  protected val companion = Node

  override def toString = "Node[%s] (nodeType: %s[%d], template: %s[%d])".format(
    id, nodeType.name, nodeTypeId, template.name, templateId
  )

  def copy(id : Option[Int] = this.id, nodeTypeId : Int = this.nodeTypeId, templateId : Int = this.templateId) = {
    new Node(id, nodeTypeId, templateId).attributes ++= this.attributes
  }


  object connectors {
    def apply() = {
      broker.transactional(Database.getConnection) {
        _.selectAll(Node.Tokens.selectByConnectee, "node" -> self)
      }.foldLeft(MuMap.empty[Int, Node]) {
        (m, n) => m(n.id.get) = n; m
      }
    }

    def having(
      connectionTypes : Iterable[ConnectionType], nodeTypes : Iterable[NodeType] = Nil
      ) = {
      broker.transactional(Database.getConnection) {
        _.selectAll(
          Node.Tokens.selectByConnectee, "node" -> self,
          "connectionTypes" -> mapToIds(connectionTypes), "connectorTypes" -> mapToIds(nodeTypes)
        )
      }.foldLeft(MuMap.empty[Int, Node]) {
        (m, n) => m(n.id.get) = n; m
      }
    }
    /*
    def having(connectionType: String, nodeType: String) : MuMap[Int, Node] = {
      having(List(ConnectionType.get(connectionType)), List(NodeType.get(nodeType)))
    }
    */
    def having(connectionType: ConnectionType, nodeType: NodeType) : MuMap[Int, Node] = {
      having(List(connectionType), List(nodeType))
    }

    def having(connectionType: ConnectionType) : MuMap[Int, Node] = {
      having(List(connectionType), Nil)
    }
  }

  object connectees {
    def apply() = {
      broker.transactional(Database.getConnection) {
        _.selectAll(Node.Tokens.selectByConnector, "node" -> self)
      }.foldLeft(MuMap.empty[Int, Node]) {
        (m, n) => m(n.id.get) = n; m
      }
    }

    def having(
      connectionTypes : Iterable[ConnectionType] = Nil, nodeTypes : Iterable[NodeType] = Nil
      ) = {
      broker.transactional(Database.getConnection) {
        _.selectAll(
          Node.Tokens.selectByConnector, "node" -> self,
          "connectionTypes" -> mapToIds(connectionTypes), "connecteeTypes" -> mapToIds(nodeTypes)
        )
      }.foldLeft(MuMap.empty[Int, Node]) {
        (m, n) => m(n.id.get) = n; m
      }
    }
    def having(connectionType: String, nodeType: String) : MuMap[Int, Node] = {
      having(List(ConnectionType.get(connectionType)), List(NodeType.get(nodeType)))
    }
  }

  lazy val nodeType = NodeType.get(nodeTypeId)
  lazy val template = Template.get(templateId)

  def attr(s : String) = {
    attributes.get(s)
  }

  def valueOf(s : String) = {
    attr(s) match {
      case Some(na) => Some(na.value)
      case None => None
    }
  }

  def connect(connectionType : ConnectionType, node : Node) : Node = {
    val connection = Connection(connectionType, this, node)
    connection.find() match {
      case Some(c) => this
      case None => {
        connection.save()
        this
      }
    }
  }

  def connect(connectionType : String, node : Node) : Node = {
    connect(ConnectionType.get(connectionType), node)
  }

  def break(connectionType : ConnectionType, node : Node) : Node = {
    val connection = Connection(connectionType, this, node)
    connection.find() match {
      case Some(c) => {
        connection.delete()
        this
      }
      case None => this
    }
  }

  def break(connectionType : String, node : Node) : Node = {
    break(ConnectionType.get(connectionType), node)
  }

  def setAttr(attribute : String, value : Int) : Node = {
    setAttr(attribute, value.toString)
  }

  def setAttr(attribute : Attribute, value : Int) : Node = {
    setAttr(attribute, value.toString)
  }

  def setAttr(attribute : Attribute, value : String) : Node = {
    setAttr(attribute.name, value)
  }

  def setAttr(attributeName : String, value : String) : Node = {
    attr(attributeName) match {
      case Some(na) => {
        val newNa = na.copy(value = value).save().get.asInstanceOf[NodeAttribute]
        attributes += newNa.attribute.name -> newNa
        this
      }
      case None => {
        val na = NodeAttribute(this, Attribute.get(attributeName), value).save().get.asInstanceOf[NodeAttribute]
        attributes += na.attribute.name -> na
        this
      }
    }
  }

  override def save() : Option[Node] = {
    id match {
      case Some(i) => throw new DaoException("Nodes cannot be updated: " + this)
      case None => {
        broker.transactional(Database.getConnection) {
          _.executeForKey[Node](Node.Tokens.insert, "node" -> this)
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

  val tableName = "nodes"
  val pK = "node_id"
  val extractor = NodeExtractor
  val columnMap = Map("node_type_id" -> "nodeTypeId", "template_id" -> "templateId")

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

  def getAll(ids : Int*) = {
    val nodes = selectAllOption(Tokens.selectByIds, "nodeIds" -> ids.toArray)
    nodes.foldLeft(MuMap.empty[Int, Node]) {
      (m, n) => m(n.id.get) = n; m
    }
  }

  def createFrom(template : Template) : Node = {
    val node = Node(template.nodeTypeId, template.id.get).save().get
    for (attr <- template.attributes) {
      node.setAttr(attr._1, attr._2.value)
    }
    node
  }

  def createFrom(template : String) : Node = {
    createFrom(Template.get(template))
  }

  def find(nodeType : NodeType, attribute : (String, String)) : Iterable[Node] = {
    broker.transactional(Database.getConnection) {
      _.selectAll(
        Node.Tokens.selectByNodeTypeAttribute, "nodeType" -> nodeType, "attribute" -> Attribute.get(attribute._1),
        "value" -> attribute._2
      )
    }
  }

  def find(nodeType : String, attribute : (String, String)) : Iterable[Node] = {
    find(NodeType.get(nodeType), (attribute._1, attribute._2))
  }

  object Tokens extends BasicTokens {
    override val selectById = Token('selectNodeById, extractor)
    val selectByIds = Token('selectNodeByIds, extractor)
    val selectByConnector = Token('selectNodesByConnector, extractor)
    val selectByConnectee = Token('selectNodesByConnectee, extractor)
    override val insert = Token('insertNode, NodeRowExtractor)
    val selectByNodeTypeAttribute = Token('selectNodeByNodeTypeAttribute, extractor)
  }
}

object NodeRowExtractor extends RowExtractor[Node] {
  val key = Set("node_id")

  def extract(row : Row) = {
    Node(
      row.integer("node_id").get,
      row.integer("node_type_id").get,
      row.integer("template_id").get
    )
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
    for (na <- join.extractSeq(NodeAttributeExtractor)) {
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