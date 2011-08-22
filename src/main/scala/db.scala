import org.orbroker._
import org.orbroker.config._
import java.io.File
import scala.collection.mutable.HashMap

package object db {

  private val URL = "jdbc:postgresql://lw-dev/logicops2"
  private val USERNAME = "logicops2"

  private val ds : javax.sql.DataSource = new SimpleDataSource(URL, "org.postgresql.Driver") {
    override def getConnection() = {
      getConnection(USERNAME, "")
    }
  }

  private val configFolder = new File("sql")
  private val builder = new BrokerBuilder(ds)
  FileSystemRegistrant(configFolder).register(builder)
  builder.verify(Tokens.idSet)
  val broker = builder.build()


  private[db] class Memoize1[-T, +R](f : T => R) extends (T => R) {

    import scala.collection.mutable

    private[this] val vals = mutable.Map.empty[T, R]

    def apply(x : T) : R = {
      if (vals.contains(x)) {
        vals(x)
      } else {
        val y = f(x)
        vals += ((x, y))
        y
      }
    }
  }

  private[db] object Memoize1 {
    def apply[T, R](f : T => R) = new Memoize1(f)
  }

  trait Dao {

    sealed trait State {
      def name : String
    }

    case object NEW extends State {
      val name = "NEW"
    }

    case object MODIFIED extends State {
      val name = "MODIFIED"
    }

    case object FROMDB extends State {
      val name = "FROMDB"
    }

  }

  trait DaoHelper[T] {
    def get(id : Int) : T = {
      getOption(id).get
    }

    val getMem = Memoize1(get)

    val getOptionMem = Memoize1(getOption)

    def getOption(id : Int) : Option[T]

    protected def selectOneOption(token : Token[T], params : (String, Any)*) = {
      broker.readOnly() {
        s =>
          s.selectOne(token, params : _*)
      }
    }
  }

  class NodeType(val id : Option[Int], val poolId : Int, val name : String) extends Dao {
    override def toString = "NodeType[%s] (name: %s, poolId: %d)".format(id, name, poolId)
  }

  object NodeType extends DaoHelper[NodeType] {
    def apply(id : Int, poolId : Int, name : String) = {
      new NodeType(Some(id), poolId, name)
    }

    def apply(poolId : Int, name : String) = {
      new NodeType(None, poolId, name)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.NodeType.selectById, "node_type_id" -> id)
    }

    def getOption(name : String) = {
      selectOneOption(Tokens.NodeType.selectByName, "interface_name" -> name)
    }

    def get(name : String) = {
      getOption(name).get
    }
  }

  object NodeTypeExtractor extends JoinExtractor[NodeType] {
    val key = Set("node_type_id")

    def extract(row : Row, join : Join) = {
      NodeType(
        row.integer("node_type_id").get,
        row.integer("node_type_pool_id").get,
        row.string("interface_name").get
      )
    }
  }

  class Template(val id : Option[Int], val nodeType : NodeType, val name : String) extends Dao {
    private[db] val attributes = new HashMap[String, TemplateAttribute]()

    override def toString = "Template[%s] (name: %s)".format(id, name)

    def getAttr(s : String) = {
      attributes.get(s) match {
        case Some(na) => Some(na.value)
        case None => None
      }
    }


  }

  object Template extends DaoHelper[Template] {
    def apply(name : String, nodeTypeId : Int) = {
      new Template(None, NodeType.getMem(nodeTypeId), name)
    }

    def apply(name : String, nodeType : NodeType) = {
      new Template(None, nodeType, name)
    }

    def apply(id : Int, nodeTypeId : Int, name : String) = {
      new Template(Some(id), NodeType.getMem(nodeTypeId), name)
    }

    def apply(id : Int, nodeType : NodeType, name : String) = {
      new Template(Some(id), nodeType, name)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.Template.selectById, "template_id" -> id)
    }

    def getOption(name : String) = {
      selectOneOption(Tokens.Template.selectByName, "interface_name" -> name)
    }

    def get(name : String) = {
      getOption(name).get
    }
  }

  object TemplateExtractor extends JoinExtractor[Template] {
    val key = Set("template_id")

    def extract(row : Row, join : Join) = {
      val template = Template(
        row.integer("template_id").get,
        row.integer("node_type_id").get,
        row.string("interface_name").get
      )

      for (ta <- join.extractSeq(TemplateAttributeExtractor)) {
        template.attributes += ta.attribute.name -> ta
      }
      template
    }
  }

  class TemplateAttribute(
    val id : Option[Int], val templateId : Int, val attribute : Attribute, val optional : Boolean, val value : String
    ) extends Dao {
    override def toString = "TemplateAttribute[%s] (template: %s[%d], attribute: %s[%d], value: %s".format(
      id, Template.getMem(templateId).name, templateId, attribute.name, attribute.id.get, value
    )
  }

  object TemplateAttribute extends DaoHelper[TemplateAttribute] {
    def apply(id : Int, template_id : Int, attribute : Attribute, optional : Boolean, value : String) = {
      new TemplateAttribute(Some(id), template_id, attribute, optional, value)
    }

    def apply(id : Int, template_id : Int, attributeId : Int, optional : Boolean, value : String) = {
      new TemplateAttribute(Some(id), template_id, Attribute.getMem(attributeId), optional, value)
    }

    def apply(template_id : Int, attribute : Attribute, optional : Boolean, value : String) = {
      new TemplateAttribute(None, template_id, attribute, optional, value)
    }

    def apply(template_id : Int, attributeId : Int, optional : Boolean, value : String) = {
      new TemplateAttribute(None, template_id, Attribute.getMem(attributeId), optional, value)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.TemplateAttribute.selectById, "template_attribute_map_id" -> id)
    }
  }

  object TemplateAttributeExtractor extends JoinExtractor[TemplateAttribute] {
    val key = Set("template_attribute_map_id")

    def extract(row : Row, join : Join) = {
      TemplateAttribute(
        row.integer("template_attribute_map_id").get,
        row.integer("template_id").get,
        row.integer("attribute_id").get,
        row.bit("optional").get,
        row.string("default_value").get
      )
    }
  }

  class Node(val id : Option[Int], val nodeTypeId : Int, val templateId : Int, private val join: Option[Join] = None) extends Dao {
    private[db] val attributes = new HashMap[String, NodeAttribute]()

    override def toString = "Node[%s] (nodeType: %s[%d], template: %s[%d])".format(
      id, nodeType.name, nodeTypeId, template.name, templateId
    )

    def connectees() = {
      broker.readOnly() {
        s => s.selectAll(Tokens.Connection.selectByConnectorNodeId, "connector_node_id" -> id)
      }
    }

    lazy val nodeType = NodeType.getMem(nodeTypeId)
    lazy val template = Template.getMem(templateId)

    def getAttr(s : String) = {
      attributes.get(s) match {
        case Some(na) => Some(na.value)
        case None => None
      }
    }
  }

  object Node extends DaoHelper[Node] {
    private def withAttributes(node : Node, join : Option[Join]) = {
      join match {
        case Some(j) =>
          for (na <- j.extractSeq(NodeAttributeExtractor)) {
            node.attributes += na.attribute.name -> na
          }
          node
        case None => node
      }
    }

    def apply(id : Int, nodeTypeId : Int, templateId : Int, join : Option[Join]) = {
      new Node(Some(id), nodeTypeId, templateId, join)
    }

    def apply(id : Int, nodeType : NodeType, templateId : Int, join : Option[Join]) = {
      apply(id, nodeType.id.get, templateId, join)
    }

    def apply(id : Int, nodeTypeId : Int, template : Template, join : Option[Join]) = {
      apply(id, nodeTypeId, template.id.get, join)
    }

    def apply(id : Int, nodeType : NodeType, template : Template, join : Option[Join]) = {
      apply(id, nodeType.id.get, template.id.get, join)
    }

    def apply(nodeTypeId : Int, templateId : Int) = {
      new Node(None, nodeTypeId, templateId)
    }

    def apply(nodeType : NodeType, templateId : Int) = {
      apply(nodeType.id.get, templateId);
    }

    def apply(nodeTypeId : Int, template : Template) = {
      apply(nodeTypeId, template.id.get)
    }

    def apply(nodeType : NodeType, template : Template) = {
      apply(nodeType.id.get, template.id.get)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.Node.selectById, "node_id" -> id)
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

      node
    }
  }

  class NodeAttribute(
    val id : Option[Int], val nodeId : Int, val attribute : Attribute, val value : String
    ) extends Dao {
    override def toString = "NodeAttribute[%s] (nodeId: %d, attribute: %s[%d], value: %s".format(
      id, nodeId, attribute.name, attribute.id.get, value
    )
  }

  object NodeAttribute extends DaoHelper[NodeAttribute] {
    def apply(id : Int, node_id : Int, attribute : Attribute, value : String) = {
      new NodeAttribute(Some(id), node_id, attribute, value)
    }

    def apply(id : Int, node_id : Int, attributeId : Int, value : String) = {
      new NodeAttribute(Some(id), node_id, Attribute.getMem(attributeId), value)
    }

    def apply(node_id : Int, attribute : Attribute, value : String) = {
      new NodeAttribute(None, node_id, attribute, value)
    }

    def apply(node_id : Int, attributeId : Int, value : String) = {
      new NodeAttribute(None, node_id, Attribute.getMem(attributeId), value)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.NodeAttribute.selectById, "node_attribute_map_id" -> id)
    }
  }

  object NodeAttributeExtractor extends JoinExtractor[NodeAttribute] {
    val key = Set("node_attribute_map_id")

    def extract(row : Row, join : Join) = {
      NodeAttribute(
        row.integer("node_attribute_map_id").get,
        row.integer("node_id").get,
        row.integer("attribute_id").get,
        row.string("value").get
      )
    }
  }

  class Attribute(val id : Option[Int], val name : String, val openended : Boolean) extends Dao {
    override def toString = "Attribute[%s] (name: %s)".format(id, name)
  }

  object Attribute extends DaoHelper[Attribute] {
    def apply(id : Int, name : String, openended : Boolean) = {
      new Attribute(Some(id), name, openended)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.Attribute.selectById, "attribute_id" -> id)
    }
  }

  object AttributeExtractor extends JoinExtractor[Attribute] {
    val key = Set("attribute_id")

    def extract(row : Row, join : Join) = {
      Attribute(
        row.integer("attribute_id").get,
        row.string("interface_name").get,
        row.bit("openended").get
      )
    }
  }

  class ConnectionType(
    val id : Option[Int], val name : String, val bidirectional : Boolean, val compliment : Int
    ) extends Dao {
    override def toString = "ConnectionType[%s] (name: %s)".format(id, name)
  }

  object ConnectionType extends DaoHelper[ConnectionType] {
    def apply(id : Int, name : String, bidirectional : Boolean, compliment : Int) = {
      new ConnectionType(Some(id), name, bidirectional, compliment)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.ConnectionType.selectById, "connection_type_id" -> id)
    }
  }

  object ConnectionTypeExtractor extends JoinExtractor[ConnectionType] {
    val key = Set("connection_type_id")

    def extract(row : Row, join : Join) = {
      ConnectionType(
        row.integer("connection_type_id").get,
        row.string("interface_name").get,
        row.bit("bidirectional").get,
        row.integer("compliment_connection_type_id").get
      )
    }
  }

  class Connection(
    val id : Option[Int], val ctype : ConnectionType, val connectorId : Int, val connectorType : NodeType,
    val connecteeId : Int, val connecteeType : NodeType
    ) extends Dao {
    override def toString = "Connection[%s] (ctype: %s[%d], connector: %d (%s[%d]), connectee: %d (%s[%d])".format(
      id, ctype.name, ctype.id.get, connectorId, connectorType.name, connectorType.id.get, connecteeId,
      connecteeType.name, connecteeType.id.get
    )
  }

  object Connection extends DaoHelper[Connection] {
    def apply(
      connectionId : Int, connectionTypeId : Int, connecteeNodeId : Int, connecteeNodeTypeId : Int,
      connectorNodeId : Int,
      connectorNodeTypeId : Int
      ) = {
      new Connection(
        Some(connectionId), ConnectionType.getMem(connectionTypeId), connecteeNodeId,
        NodeType.getMem(connecteeNodeTypeId),
        connectorNodeId,
        NodeType.getMem(connectorNodeTypeId)
      )
    }

    def apply(
      connectionId : Int, connectionType : ConnectionType, connecteeNodeId : Int, connecteeNodeType : NodeType,
      connectorNodeId : Int,
      connectorNodeType : NodeType
      ) = {
      new Connection(
        Some(connectionId), connectionType, connecteeNodeId, connecteeNodeType,
        connectorNodeId,
        connectorNodeType
      )
    }

    def apply(
      connectionTypeId : Int, connecteeNodeId : Int, connecteeNodeTypeId : Int, connectorNodeId : Int,
      connectorNodeTypeId : Int
      ) = {
      new Connection(
        None, ConnectionType.getMem(connectionTypeId), connecteeNodeId, NodeType.getMem(connecteeNodeTypeId),
        connectorNodeId,
        NodeType.getMem(connectorNodeTypeId)
      )
    }

    def apply(
      connectionType : ConnectionType, connecteeNodeId : Int, connecteeNodeType : NodeType,
      connectorNodeId : Int,
      connectorNodeType : NodeType
      ) = {
      new Connection(
        None, connectionType, connecteeNodeId, connecteeNodeType,
        connectorNodeId,
        connectorNodeType
      )
    }

    def getOption(id : Int) = {
      broker.readOnly() {
        s =>
          s.selectOne(Tokens.Connection.selectById, "connection_id" -> id)
      }
    }
  }

  object ConnectionExtractor extends JoinExtractor[Connection] {
    val key = Set("connection_id")

    def extract(row : Row, join : Join) = {
      Connection(
        row.integer("connection_id").get,
        join.extractOne(ConnectionTypeExtractor, Map("interface_name" -> "connection_type_name")).get,
        row.integer("connector_node_id").get,
        join.extractOne(
          NodeTypeExtractor, Map(
            "interface_name" -> "connector_node_type_name", "node_type_id" -> "connector_node_type_id",
            "node_type_pool_id" -> "connector_node_type_pool_id"
          )
        ).get,
        row.integer("connectee_node_id").get,
        join.extractOne(
          NodeTypeExtractor, Map(
            "interface_name" -> "connectee_node_type_name", "node_type_id" -> "connectee_node_type_id",
            "node_type_pool_id" -> "connectee_node_type_pool_id"
          )
        ).get
      )
    }
  }

  object Tokens extends TokenSet(true) {

    object Node {
      val selectById = Token('selectNodeById, NodeExtractor)
    }

    object NodeType {
      val selectById = Token('selectNodeTypeById, NodeTypeExtractor)
      val selectByName = Token('selectNodeTypeByName, NodeTypeExtractor)
    }

    object NodeAttribute {
      val selectById = Token('selectNodeAttributeById, NodeAttributeExtractor)
    }

    object Connection {
      val selectById = Token('selectConnectionById, ConnectionExtractor)
      val selectByConnectorNodeId = Token('selectConnectionsByConnectorNodeId, ConnectionExtractor)
      val selectByConnecteeNodeId = Token('selectConnectionsByConnecteeNodeId, ConnectionExtractor)
    }

    object ConnectionType {
      val selectById = Token('selectConnectionTypeById, ConnectionTypeExtractor)
      val selectByName = Token('selectConnectionTypeByName, ConnectionTypeExtractor)
    }

    object Template {
      val selectById = Token('selectTemplateById, TemplateExtractor)
      val selectByName = Token('selectTemplateByName, TemplateExtractor)
    }

    object TemplateAttribute {
      val selectById = Token('selectTemplateAttributeById, TemplateAttributeExtractor)
    }

    object Attribute {
      val selectById = Token('selectAttributeById, AttributeExtractor)
      val selectByName = Token('selectAttributeByName, AttributeExtractor)
    }

  }

}

