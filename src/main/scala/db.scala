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
    def apply(id : Int, poolId : Int, name : String): NodeType = {
      new NodeType(Some(id), poolId, name)
    }

    def apply(poolId : Int, name : String): NodeType = {
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

    /* TODO
      def allByPoolId(poolId: Int): IndexedSeq[NodeType] {}
     */
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

  class Template(val id : Option[Int], val nodeTypeId : Int, val name : String) extends Dao {
    private[db] val attributes = new HashMap[String, TemplateAttribute]()

    override def toString = "Template[%s] (name: %s, nodeType: %s[%d])".format(id, name, nodeTypeId, nodeType.name)

    lazy val nodeType = NodeType.getMem(nodeTypeId)

    def getAttr(s : String) = {
      attributes.get(s) match {
        case Some(na) => Some(na.value)
        case None => None
      }
    }
    /* TODO
      def children(): IndexedSeq[Template] {}
      def allAttrs(): IndexedSeq[TemplateAttribute] {}
     */
  }

  object Template extends DaoHelper[Template] {
    def apply(nodeTypeId : Int, name : String): Template = {
      new Template(None, nodeTypeId, name)
    }

    def apply(nodeType : NodeType, name : String): Template = {
      apply(nodeType.id.get, name)
    }

    def apply(id : Int, nodeTypeId : Int, name : String): Template = {
      new Template(Some(id), nodeTypeId, name)
    }

    def apply(id : Int, nodeType : NodeType, name : String): Template = {
      apply(id, nodeType.id.get, name)
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
    /* TODO:
      def all(nodeType: NodeType*)
      def all()
      def all(attribute: Attribute*)
      def all(attribute_values: Map[Attribute, String])

      def filter(nodeType: NodeType*)
      def filter(attribute: Attribute*)
      def filter(attribute_values: Map[Attribute, String])
    */
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
    val id : Option[Int], val templateId : Int, val attributeId : Int, val optional : Boolean, val value : String
    ) extends Dao {
    override def toString = "TemplateAttribute[%s] (template: %s[%d], attribute: %s[%d], value: %s".format(
      id, template.name, templateId, attribute.name, attributeId, value
    )

    lazy val template = Template.getMem(templateId)
    lazy val attribute = Attribute.getMem(attributeId)
  }

  object TemplateAttribute extends DaoHelper[TemplateAttribute] {

    def apply(id : Int, template_id : Int, attributeId : Int, optional : Boolean, value : String): TemplateAttribute = {
      new TemplateAttribute(Some(id), template_id, attributeId, optional, value)
    }

    def apply(id : Int, template_id : Int, attribute : Attribute, optional : Boolean, value : String): TemplateAttribute = {
      apply(id, template_id, attribute.id.get, optional, value)
    }

    def apply(template_id : Int, attributeId : Int, optional : Boolean, value : String): TemplateAttribute = {
      new TemplateAttribute(None, template_id, attributeId, optional, value)
    }

    def apply(template_id : Int, attribute : Attribute, optional : Boolean, value : String): TemplateAttribute = {
      apply(template_id, attribute.id.get, optional, value)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.TemplateAttribute.selectById, "template_attribute_map_id" -> id)
    }
    /* TODO:
      def all(attribute: Attribute*): IndexedSeq[TemplateAttribute]
      def all(template: Template*): IndexedSeq[TemplateAttribute]
     */
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

    def connectors() = {
      broker.readOnly() { s =>
        s.selectAll(Tokens.Connection.selectByConnecteeNodeId, "connectee_node_id" -> id)
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

    def apply(id : Int, nodeTypeId : Int, templateId : Int, join : Option[Join]): Node = {
      new Node(Some(id), nodeTypeId, templateId, join)
    }

    def apply(id : Int, nodeType : NodeType, templateId : Int, join : Option[Join]): Node = {
      apply(id, nodeType.id.get, templateId, join)
    }

    def apply(id : Int, nodeTypeId : Int, template : Template, join : Option[Join]): Node = {
      apply(id, nodeTypeId, template.id.get, join)
    }

    def apply(id : Int, nodeType : NodeType, template : Template, join : Option[Join]): Node = {
      apply(id, nodeType.id.get, template.id.get, join)
    }

    def apply(nodeTypeId : Int, templateId : Int): Node = {
      new Node(None, nodeTypeId, templateId)
    }

    def apply(nodeType : NodeType, templateId : Int): Node = {
      apply(nodeType.id.get, templateId);
    }

    def apply(nodeTypeId : Int, template : Template): Node = {
      apply(nodeTypeId, template.id.get)
    }

    def apply(nodeType : NodeType, template : Template): Node = {
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
        row.integer("template_id").get,
        Some(join)
      )

      node
    }
  }

  class NodeAttribute(
    val id : Option[Int], val nodeId : Int, val attributeId : Int, val value : String
    ) extends Dao {
    override def toString = "NodeAttribute[%s] (nodeId: %d, attribute: %s[%d], value: %s".format(
      id, nodeId, Attribute.get(attributeId).name, attributeId, value
    )

    lazy val attribute = Attribute.getMem(attributeId)
  }

  object NodeAttribute extends DaoHelper[NodeAttribute] {
    def apply(id : Int, node_id : Int, attribute : Attribute, value : String): NodeAttribute = {
      new NodeAttribute(Some(id), node_id, attribute.id.get, value)
    }

    def apply(id : Int, node_id : Int, attributeId : Int, value : String): NodeAttribute = {
      new NodeAttribute(Some(id), node_id, attributeId, value)
    }

    def apply(node_id : Int, attribute : Attribute, value : String): NodeAttribute = {
      new NodeAttribute(None, node_id, attribute.id.get, value)
    }

    def apply(node_id : Int, attributeId : Int, value : String): NodeAttribute = {
      new NodeAttribute(None, node_id, attributeId, value)
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
    def apply(id : Int, name : String, openended : Boolean): Attribute = {
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
    val id : Option[Int], val name : String, val bidirectional : Boolean, val complimentTypeId : Int
    ) extends Dao {
    override def toString = "ConnectionType[%s] (name: %s)".format(id, name)
    lazy val complimentType = ConnectionType.getMem(complimentTypeId)
  }

  object ConnectionType extends DaoHelper[ConnectionType] {
    def apply(id : Int, name : String, bidirectional : Boolean, complimentTypeId : Int): ConnectionType = {
      new ConnectionType(Some(id), name, bidirectional, complimentTypeId)
    }
    def apply(id : Int, name : String, bidirectional : Boolean, complimentType : ConnectionType): ConnectionType = {
      apply(id, name, bidirectional, complimentType.id.get)
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
    val id : Option[Int], val connectionTypeId : Int, val connectorId : Int, val connectorTypeId : Int,
    val connecteeId : Int, val connecteeTypeId : Int
    ) extends Dao {
    override def toString = "Connection[%s] (type: %s[%d], connector: %d (%s[%d]), connectee: %d (%s[%d])".format(
      id, ConnectionType.getMem(connectionTypeId), connectionTypeId, connectorId, NodeType.getMem(connectorTypeId).name, connectorTypeId, connecteeId,
      NodeType.getMem(connecteeTypeId), connecteeTypeId
    )
    lazy val connecteeType = NodeType.getMem(connecteeTypeId)
    lazy val connectorType = NodeType.getMem(connectorTypeId)
    lazy val connectionType = ConnectionType.getMem(connectionTypeId)
  }

  object Connection extends DaoHelper[Connection] {
    def apply(
      connectionId : Int, connectionTypeId : Int, connecteeId : Int, connecteeTypeId : Int,
      connectorId : Int,
      connectorTypeId : Int
      ): Connection = {
      new Connection(
        Some(connectionId), connectionTypeId, connecteeId,
        connecteeTypeId,
        connectorId,
        connectorTypeId
      )
    }

    def apply(
      connectionId : Int, connectionType : ConnectionType, connecteeId : Int, connecteeType : NodeType,
      connectorId : Int,
      connectorType : NodeType
      ): Connection = {
      apply(
        connectionId, connectionType.id.get, connecteeId, connecteeType.id.get,
        connectorId,
        connectorType.id.get
      )
    }

    def apply(
      connectionTypeId : Int, connecteeId : Int, connecteeTypeId : Int, connectorId : Int,
      connectorTypeId : Int
      ): Connection = {
      new Connection(
        None, connectionTypeId, connecteeId, connecteeTypeId,
        connectorId,
        connectorTypeId
      )
    }

    def apply(
      connectionType : ConnectionType, connecteeId : Int, connecteeType : NodeType,
      connectorId : Int,
      connectorType : NodeType
      ): Connection = {
      apply(connectionType.id.get, connecteeId, connecteeType.id.get,
        connectorId,
        connectorType.id.get
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

