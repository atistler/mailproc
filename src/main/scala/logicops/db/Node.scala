package logicops.db {

import org.orbroker._
import scala.collection.mutable.{Map => MuMap}

private[db] trait LikeHaving {
  def apply() = having()

  def having(connectionTypes : Iterable[ConnectionType] = Nil, nodeTypes : Iterable[NodeType] = Nil) : MuMap[Int, Node]

  def having(connectionType : ConnectionType, nodeType : NodeType) : MuMap[Int, Node] = {
    having(List(connectionType), List(nodeType))
  }

  def having(connectionType : ConnectionType) : MuMap[Int, Node] = {
    having(List(connectionType), Nil)
  }
}

private[db] trait ChildParent {
  val connectors: LikeHaving
  val connectees: LikeHaving

  def children(nodeTypes: Iterable[NodeType] = Nil) : MuMap[Int, Node] = {
    connectors.having(List(ConnectionType.get("Child")), nodeTypes)
  }
  def children(nodeType: NodeType) : MuMap[Int, Node] = {
    children(List(nodeType))
  }
  def parents(nodeTypes: Iterable[NodeType] = Nil) : MuMap[Int, Node] = {
    connectors.having(List(ConnectionType.get("Parent")), nodeTypes)
  }
  def parents(nodeType: NodeType) : MuMap[Int, Node] = {
    parents(List(nodeType))
  }
  def child(nodeTypes: Iterable[NodeType] = Nil) : Option[(Int, Node)] = {
    children(nodeTypes).headOption
  }
  def child(nodeType: NodeType) : Option[(Int, Node)] = {
    child(List(nodeType))
  }
  def parent(nodeTypes: Iterable[NodeType] = Nil) : Option[(Int, Node)] = {
    parents(nodeTypes).headOption
  }
  def parent(nodeType: NodeType) : Option[(Int, Node)] = {
    parent(List(nodeType))
  }

}

class Nodes(nodeMap : MuMap[Int, Node]) extends ChildParent {
  object connectors extends LikeHaving {
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
  }

  object connectees extends LikeHaving {
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
  }

}

class Node(val id : Option[Int], val nodeTypeId : Int, val templateId : Int) extends Dao[Node] with ChildParent {
  private[db] val attributes = MuMap.empty[Attribute, NodeAttribute]

  private val self = this

  protected val companion = Node

  override def toString = "Node[%s] (nodeType: %s[%d], template: %s[%d])".format(
    id, nodeType.name, nodeTypeId, template.name, templateId
  )

  def copy(id : Option[Int] = this.id, nodeTypeId : Int = this.nodeTypeId, templateId : Int = this.templateId) = {
    new Node(id, nodeTypeId, templateId).attributes ++= this.attributes
  }


  object connectors extends LikeHaving {
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
  }

  object connectees extends LikeHaving {
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
  }

  lazy val nodeType = NodeType.get(nodeTypeId)
  lazy val template = Template.get(templateId)

  def attrs = attributes

  def attr(a : Attribute) = {
    attributes.get(a)
  }

  def valueOf(a : Attribute) = {
    attr(a) match {
      case Some(na) => Some(na.value)
      case None => {
        template.attr(a) match {
          case Some(ta) => Some(ta.value)
          case None => None
        }
      }
    }
  }

  def addParent(node : Node) : Node = {
    node.connect("Parent", this)
    this
  }

  def addChild(node : Node) : Node = {
    node.connect("Child", this)
    this
  }

  def connect(connectionType : ConnectionType, node : Node) : Node = {
    val connection = Connection(connectionType, this, node)
    connection.find() match {
      case Some(c) => this
      case None => {
        println("Adding connection %s".format(connection))
        connection.save()
        if ( connectionType.bidirectional ) {
          val compliment = Connection(connectionType.complimentTypeId, node, this)
          compliment.find() match {
            case Some(c) => this
            case None => {
              compliment.save()
              println("Adding compliment %s".format(compliment))
              this
            }
          }
        }
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
        println("Breaking connection: %s".format(connection))
        connection.delete()
        this
      }
      case None => this
    }
  }

  def break(connectionType : String, node : Node) : Node = {
    break(ConnectionType.get(connectionType), node)
  }

  def setAttr(a : Attribute, value : Int) : Node = {
    setAttr(a, value.toString)
  }


  def setAttr(a : Attribute, value : String) : Node = {
    attr(a) match {
      case Some(na : NodeAttribute) => {
        val newNa = na.copy(value = value).save().get
        attributes += newNa.attribute -> newNa
        this
      }
      case None => {
        val na = NodeAttribute(this, a, value).save().get
        attributes += na.attribute -> na
        this
      }
    }
  }

  override def save() = {
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

  def apply(nodeTypeId : Int, templateId : Int) : Node = {
    new Node(None, nodeTypeId, templateId)
  }

  def apply(nodeType : NodeType, template : Template) : Node = {
    apply(nodeType.id.get, template.id.get)
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

  def find(nodeType : NodeType, attribute : (Attribute, String)) : Iterable[Node] = {
    broker.transactional(Database.getConnection) {
      _.selectAll(
        Node.Tokens.selectByNodeTypeAttribute, "nodeType" -> nodeType, "attribute" -> attribute._1,
        "value" -> attribute._2
      )
    }
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
      node.attributes += na.attribute -> na
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