import _root_.db.DaoException
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

  class ConnectionSeq(iseq : IndexedSeq[Connection]) {
    def nodes() {
      iseq foreach {
        c => println(c)
      }
    }
  }

  implicit def IndexedSeq2ConnectionSeq(i : IndexedSeq[Connection]) = {
    new ConnectionSeq(i);
  }

  class NodeSeq(nseq : IndexedSeq[Node]) {
    def nodes() {
      nseq foreach {
        n => println(n)
      }
    }
  }

  implicit def IndexedSeq2NodeSeq(n : IndexedSeq[Node]) = {
    new NodeSeq(n)
  }

  private val configFolder = new File("sql")
  private val builder = new BrokerBuilder(ds) with dynamic.FreeMarkerSupport
  FileSystemRegistrant(configFolder).register(builder)
  builder.verify(Tokens.idSet)
  val broker = builder.build()

  def mapToIds(d : List[Dao]) = {
    d.map(_.id.get)
  }

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

  private[db] class Def[C](implicit desired : Manifest[C]) {
    def unapply[X](c : X)(implicit m : Manifest[X]) : Option[C] = {
      def sameArgs =
        desired.typeArguments.zip(m.typeArguments).forall {
          case (desired, actual) => desired >:> actual
        }
      if (desired >:> m && sameArgs) Some(c.asInstanceOf[C])
      else None
    }
  }

  class DaoException(msg : String) extends Exception(msg)

  trait Dao {

    var id : Option[Int]

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
        _.selectOne(token, params : _*)
      }
    }

    protected def selectAllOption(token : Token[T], params : (String, Any)*) = {
      broker.readOnly() {
        _.selectAll(token, params : _*)
      }
    }
  }

  class NodeType(var id : Option[Int], var poolId : Int, var name : String) extends Dao {
    override def toString = "NodeType[%s] (name: %s, poolId: %d)".format(id, name, poolId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            /* TODO: NodeTypes should really never by updated ( maybe just interface_name ) */
            case Some(_id) => t.callForUpdate(
              Tokens.NodeType.update, "node_type_id" -> _id, "node_type_pool_id" -> poolId, "interface_name" -> name
            )
            case None => t.callForKeys(Tokens.NodeType.insert, "node_type_pool_id" -> id, "interface_name" -> name) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object NodeType extends DaoHelper[NodeType] {
    def apply(id : Int, poolId : Int, name : String) : NodeType = {
      new NodeType(Some(id), poolId, name)
    }

    def apply(poolId : Int, name : String) : NodeType = {
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

    def allByPoolId(poolId : Int) = {
      broker.readOnly() {
        s => s.selectAll(Tokens.NodeType.selectByPoolId, "node_type_pool_id" -> poolId)
      }
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

  class Template(var id : Option[Int], var nodeTypeId : Int, var name : String) extends Dao {
    private[db] val attributes = new HashMap[String, TemplateAttribute]()

    override def toString = "Template[%s] (name: %s, nodeType: %s[%d])".format(id, name, nodeTypeId, nodeType.name)

    lazy val nodeType = NodeType.getMem(nodeTypeId)

    def getAttr(s : String) = {
      attributes.get(s) match {
        case Some(na) => Some(na.value)
        case None => None
      }
    }

    def allAttrs = attributes

    def save() = {
      broker.transaction() {
        t =>
          id match {
            /* TODO: Templates should never really be updated ( maybe just interface_name ) */
            case Some(_id) => t.callForUpdate(
              Tokens.Template.update, "template_id" -> _id, "node_type_id" -> nodeType.id.get, "interface_name" -> name
            )
            case None => t.callForKeys(Tokens.Template.insert, "node_type_id" -> id, "interface_name" -> name) {
              k : Int => id = Some(k)
            }
          }
      }
    }

    /*
   TODO: def children(): IndexedSeq[Template] {}
    */
  }

  object Template extends DaoHelper[Template] {
    def apply(nodeTypeId : Int, name : String) : Template = {
      new Template(None, nodeTypeId, name)
    }

    def apply(nodeType : NodeType, name : String) : Template = {
      apply(nodeType.id.get, name)
    }

    def apply(id : Int, nodeTypeId : Int, name : String) : Template = {
      new Template(Some(id), nodeTypeId, name)
    }

    def apply(id : Int, nodeType : NodeType, name : String) : Template = {
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

    def all(nodeTypes : List[NodeType] = Nil, attributes : List[Attribute] = Nil) : IndexedSeq[Template] = {
      broker.readOnly() {
        s => s.selectAll(
          Tokens.Template.selectByQuery,
          "nodeTypeIds" -> mapToIds(nodeTypes).toArray, "attributeIds" -> mapToIds(attributes).toArray
        )
      }
    }

    def all[T](t : T*)(implicit m : Manifest[T]) : IndexedSeq[Template] = {
      val clazz = Class.forName(m.toString)
      if (clazz.isAssignableFrom(Attribute.getClass)) {
        broker.readOnly() {
          s => s.selectAll(
            Tokens.Template.selectByQuery,
            "attributeIds" -> t.toArray
          )
        }
      } else if (clazz.isAssignableFrom(NodeType.getClass)) {
        broker.readOnly() {
          s => s.selectAll(
            Tokens.Template.selectByQuery,
            "nodeTypeIds" -> t.toArray
          )
        }
      } else {
        IndexedSeq.empty[Template]
      }
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
    var id : Option[Int], var templateId : Int, var attributeId : Int, var optional : Boolean, var value : String
    ) extends Dao {
    override def toString = "TemplateAttribute[%s] (template: %s[%d], attribute: %s[%d], value: %s".format(
      id, template.name, templateId, attribute.name, attributeId, value
    )

    lazy val template = Template.getMem(templateId)
    lazy val attribute = Attribute.getMem(attributeId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => t.callForUpdate(
              Tokens.TemplateAttribute.update, "template_attribute_map_id" -> _id, "template_id" -> template.id.get,
              "attribute_id" -> attribute.id.get, "optional" -> optional, "default_value" -> value
            )
            case None => t.callForKeys(
              Tokens.TemplateAttribute.insert, "template_id" -> template.id.get, "attribute_id" -> attribute.id.get,
              "optional" -> optional, "default_value" -> value
            ) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object TemplateAttribute extends DaoHelper[TemplateAttribute] {

    def apply(
      id : Int, template_id : Int, attributeId : Int, optional : Boolean, value : String
      ) : TemplateAttribute = {
      new TemplateAttribute(Some(id), template_id, attributeId, optional, value)
    }

    def apply(
      id : Int, template_id : Int, attribute : Attribute, optional : Boolean, value : String
      ) : TemplateAttribute = {
      apply(id, template_id, attribute.id.get, optional, value)
    }

    def apply(template_id : Int, attributeId : Int, optional : Boolean, value : String) : TemplateAttribute = {
      new TemplateAttribute(None, template_id, attributeId, optional, value)
    }

    def apply(template_id : Int, attribute : Attribute, optional : Boolean, value : String) : TemplateAttribute = {
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

  class Node(var id : Option[Int], var nodeTypeId : Int, var templateId : Int) extends Dao {
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
      broker.readOnly() {
        s =>
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

    def get(ids : List[Int]) = {
      selectAllOption(Tokens.Node.selectByIds, "nodeIds" -> ids)
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

  class NodeAttribute(
    var id : Option[Int], var nodeId : Int, var attributeId : Int, var value : String
    ) extends Dao {
    override def toString = "NodeAttribute[%s] (nodeId: %d, attribute: %s[%d], value: %s".format(
      id, nodeId, Attribute.get(attributeId).name, attributeId, value
    )

    lazy val attribute = Attribute.getMem(attributeId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => t.callForUpdate(
              Tokens.NodeAttribute.update, "node_attribute_map_id" -> _id, "node_id" -> nodeId,
              "attribute_id" -> attributeId, "value" -> value
            )
            case None => t.callForKeys(
              Tokens.NodeAttribute.insert, "node_id" -> nodeId, "attribute_id" -> attributeId, "value" -> value
            ) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object NodeAttribute extends DaoHelper[NodeAttribute] {
    def apply(id : Int, node_id : Int, attribute : Attribute, value : String) : NodeAttribute = {
      new NodeAttribute(Some(id), node_id, attribute.id.get, value)
    }

    def apply(id : Int, node_id : Int, attributeId : Int, value : String) : NodeAttribute = {
      new NodeAttribute(Some(id), node_id, attributeId, value)
    }

    def apply(node_id : Int, attribute : Attribute, value : String) : NodeAttribute = {
      new NodeAttribute(None, node_id, attribute.id.get, value)
    }

    def apply(node_id : Int, attributeId : Int, value : String) : NodeAttribute = {
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

  class AttributeOption(var id : Option[Int], var attributeId : Int, var value : String) extends Dao {
    override def toString = "AttributeOption[%s] (attribute: %s[%d], value: %s".format(
      id.get, attribute.name, attributeId, value
    )

    lazy val attribute = Attribute.getMem(attributeId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => t.callForUpdate(
              Tokens.AttributeOption.update, "attribute_option_id" -> _id, "attribute_id" -> attributeId,
              "value" -> value
            )
            case None => t.callForKeys(Tokens.AttributeOption.insert, "attribute_id" -> attributeId, "value" -> value) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object AttributeOption extends DaoHelper[AttributeOption] {
    def apply(id : Int, attributeId : Int, value : String) : AttributeOption = {
      new AttributeOption(Some(id), attributeId, value)
    }

    def apply(id : Int, attribute : Attribute, value : String) : AttributeOption = {
      apply(id, attribute.id.get, value)
    }

    def apply(attributeId : Int, value : String) : AttributeOption = {
      new AttributeOption(None, attributeId, value)
    }

    def apply(attribute : Attribute, value : String) : AttributeOption = {
      apply(attribute.id.get, value)
    }

    def getOption(id : Int) = {
      selectOneOption(Tokens.AttributeOption.selectById, "attribute_option_id" -> id)
    }
  }

  object AttributeOptionExtractor extends JoinExtractor[AttributeOption] {
    val key = Set("attribute_option_id")

    def extract(row : Row, join : Join) = {
      AttributeOption(
        row.integer("attribute_option_id").get,
        row.integer("attribute_id").get,
        row.string("value").get
      )
    }
  }

  class Attribute(var id : Option[Int], var name : String, var openended : Boolean = true) extends Dao {
    override def toString = "Attribute[%s] (name: %s)".format(id, name)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => t.callForUpdate(
              Tokens.Attribute.update, "attribute_id" -> _id, "interface_name" -> name, "openended" -> openended
            )
            case None => t.callForKeys(Tokens.NodeAttribute.insert, "interface_name" -> name, "openended" -> openended)
            {
              k : Int => id = Some(k)
            }
          }
      }
    }

    lazy val options = {
      broker.readOnly() {
        _.selectAll(Tokens.AttributeOption.selectOptionsByAttributeId, "attribute_id" -> id.get)
      }
    }
  }

  object Attribute extends DaoHelper[Attribute] {
    def apply(id : Int, name : String, openended : Boolean) : Attribute = {
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
    var id : Option[Int], var name : String, var bidirectional : Boolean = false, var complimentTypeId : Int = 0
    ) extends Dao {
    override def toString = "ConnectionType[%s] (name: %s)".format(id, name)

    lazy val complimentType = ConnectionType.getMem(complimentTypeId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => t.callForUpdate(
              Tokens.ConnectionType.update, "connection_type_id" -> _id, "interface_name" -> name,
              "bidirectional" -> bidirectional, "compliment_connection_type_id" -> complimentType.id.get
            )
            case None => t.callForKeys(
              Tokens.ConnectionType.insert, "interface_name" -> name, "bidirectional" -> bidirectional,
              "compliment_connection_type_id" -> complimentType.id.get
            ) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object ConnectionType extends DaoHelper[ConnectionType] {
    def apply(id : Int, name : String, bidirectional : Boolean, complimentTypeId : Int) : ConnectionType = {
      new ConnectionType(Some(id), name, bidirectional, complimentTypeId)
    }

    def apply(id : Int, name : String, bidirectional : Boolean, complimentType : ConnectionType) : ConnectionType = {
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
    var id : Option[Int], var connectionTypeId : Int, var connectorId : Int, var connectorTypeId : Int,
    var connecteeId : Int, var connecteeTypeId : Int
    ) extends Dao {
    override def toString = "Connection[%s] (type: %s[%d], connector: %d (%s[%d]), connectee: %d (%s[%d])".format(
      id, ConnectionType.getMem(connectionTypeId), connectionTypeId, connectorId, NodeType.getMem(connectorTypeId).name,
      connectorTypeId, connecteeId,
      NodeType.getMem(connecteeTypeId), connecteeTypeId
    )

    lazy val connecteeType = NodeType.getMem(connecteeTypeId)
    lazy val connectorType = NodeType.getMem(connectorTypeId)
    lazy val connectionType = ConnectionType.getMem(connectionTypeId)

    val connectee = Node.get(connecteeId)
    val connector = Node.get(connectorId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => throw new DaoException("Connections cannot be updated: " + this)
            case None => t.callForKeys(
              Tokens.Connection.insert, "connection_type_id" -> connectionType.id.get,
              "connector_node_id" -> connectorId,
              "connector_node_type_id" -> connectorType.id.get, "connectee_node_id" -> connecteeId,
              "connectee_node_type_id" -> connecteeType.id.get
            ) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object Connection extends DaoHelper[Connection] {
    def apply(
      connectionId : Int, connectionTypeId : Int, connecteeId : Int, connecteeTypeId : Int,
      connectorId : Int,
      connectorTypeId : Int
      ) : Connection = {
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
      ) : Connection = {
      apply(
        connectionId, connectionType.id.get, connecteeId, connecteeType.id.get,
        connectorId,
        connectorType.id.get
      )
    }

    def apply(
      connectionId : Int, connectionTypeId : Int, connectee : Node, connecteeTypeId : Int,
      connector : Node,
      connectorTypeId : Int
      ) : Connection = {
      apply(
        connectionId, connectionTypeId, connectee.id.get,
        connecteeTypeId,
        connector.id.get,
        connectorTypeId
      )
    }

    def apply(
      connectionId : Int, connectionType : ConnectionType, connectee : Node, connecteeType : NodeType,
      connector : Node,
      connectorType : NodeType
      ) : Connection = {
      apply(
        connectionId, connectionType.id.get, connectee.id.get, connecteeType.id.get,
        connector.id.get,
        connectorType.id.get
      )
    }

    def apply(
      connectionTypeId : Int, connecteeId : Int, connecteeTypeId : Int, connectorId : Int,
      connectorTypeId : Int
      ) : Connection = {
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
      ) : Connection = {
      apply(
        connectionType.id.get, connecteeId, connecteeType.id.get,
        connectorId,
        connectorType.id.get
      )
    }

    def apply(
      connectionTypeId : Int, connectee : Node, connecteeTypeId : Int, connector : Node,
      connectorTypeId : Int
      ) : Connection = {
      new Connection(
        None, connectionTypeId, connectee.id.get, connecteeTypeId,
        connector.id.get,
        connectorTypeId
      )
    }

    def apply(
      connectionType : ConnectionType, connectee : Node, connecteeType : NodeType,
      connector : Node,
      connectorType : NodeType
      ) : Connection = {
      apply(
        connectionType.id.get, connectee.id.get, connecteeType.id.get,
        connector.id.get,
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
        row.integer("connection_type_id").get,
        row.integer("connector_node_id").get,
        row.integer("connector_node_type_id").get,
        row.integer("connectee_node_id").get,
        row.integer("connectee_node_type_id").get
      )
    }
  }

  object Tokens extends TokenSet(true) {

    object Node {
      val selectById = Token('selectNodeById, NodeExtractor)
      val selectByIds = Token('selectNodeByIds, NodeExtractor)
      val insert = Token('insertNode)
    }

    object NodeType {
      val selectById = Token('selectNodeTypeById, NodeTypeExtractor)
      val selectByName = Token('selectNodeTypeByName, NodeTypeExtractor)
      val selectByPoolId = Token('selectNodeTypesByPoolId, NodeTypeExtractor)
      val update = Token('updateNodeType)
      val insert = Token('insertNodeType)
    }

    object NodeAttribute {
      val selectById = Token('selectNodeAttributeById, NodeAttributeExtractor)
      val insert = Token('insertNodeAttribute)
      val update = Token('updateNodeAttribute)
    }

    object Connection {
      val selectById = Token('selectConnectionById, ConnectionExtractor)
      val selectByConnectorNodeId = Token('selectConnectionsByConnectorNodeId, ConnectionExtractor)
      val selectByConnecteeNodeId = Token('selectConnectionsByConnecteeNodeId, ConnectionExtractor)
      val insert = Token('insertConnection)
    }

    object ConnectionType {
      val selectById = Token('selectConnectionTypeById, ConnectionTypeExtractor)
      val selectByName = Token('selectConnectionTypeByName, ConnectionTypeExtractor)
      val insert = Token('insertConnectionType)
      val update = Token('updateConnectionType)
    }

    object Template {
      val selectById = Token('selectTemplateById, TemplateExtractor)
      val selectByName = Token('selectTemplateByName, TemplateExtractor)
      val selectByQuery = Token('selectTemplateByQuery, TemplateExtractor)
      val update = Token('updateTemplate)
      val insert = Token('insertTemplate)
    }

    object TemplateAttribute {
      val selectById = Token('selectTemplateAttributeById, TemplateAttributeExtractor)
      val update = Token('updateTemplateAttribute)
      val insert = Token('insertTemplateAttribute)
    }

    object Attribute {
      val selectById = Token('selectAttributeById, AttributeExtractor)
      val selectByName = Token('selectAttributeByName, AttributeExtractor)
      val update = Token('updateAttribute)
      val insert = Token('insertAttribute)
    }

    object AttributeOption {
      val selectById = Token('selectAttributeOptionById, AttributeOptionExtractor)
      val selectOptionsByAttributeId = Token('selectAttributeOptionsByAttributeId, AttributeOptionExtractor)
      val update = Token('updateAttributeOption)
      val insert = Token('insertAttributeOption)
    }

  }

}

