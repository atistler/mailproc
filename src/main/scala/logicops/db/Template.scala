package logicops.db {

import org.orbroker._
import scala.collection.mutable.{Map => MuMap}

class Template(val id : Option[Int], val nodeTypeId : Int, val name : String) extends Dao[Template] {
  private[db] val attributes = MuMap.empty[String, TemplateAttribute]

  override def toString = "Template[%s] (name: %s, nodeType: %s[%d])".format(id, name, nodeType.name, nodeTypeId)

  protected val companion = Template

  def copy(id : Option[Int] = this.id, nodeTypeId : Int = this.nodeTypeId, name : String = this.name) = {
    new Template(id, nodeTypeId, name)
  }

  lazy val nodeType = NodeType.getMem(nodeTypeId)

  def attr(s : String) = {
    attributes.get(s)
  }

  def valueOf(s : String) = {
    attr(s) match {
      case Some(na) => Some(na.value)
      case None => None
    }
  }

  /*
 TODO: def children(): IndexedSeq[Template] {}
  */
}

object Template extends NamedDaoHelper[Template] {

  val pK = "template_id"
  val tableName = "templates"
  val extractor = TemplateExtractor
  val columnMap = Map("node_type_id" -> "nodeTypeId", "interface_name" -> "name")

  def apply(nodeTypeId : Int, name : String) : Template = {
    new Template(None, nodeTypeId, name)
  }

  def apply(nodeType : NodeType, name : String) : Template = {
    apply(nodeType.id.get, name)
  }

  def apply(nodeType : String, name : String) : Template = {
    apply(NodeType.get(nodeType), name)
  }

  def apply(id : Int, nodeTypeId : Int, name : String) : Template = {
    new Template(Some(id), nodeTypeId, name)
  }

  def apply(id : Int, nodeType : NodeType, name : String) : Template = {
    apply(id, nodeType.id.get, name)
  }

  def apply(id : Int, nodeType : String, name : String) : Template = {
    apply(id, NodeType.get(nodeType), name)
  }

  def all(nodeTypes : List[NodeType] = Nil, attributes : List[Attribute] = Nil) : IndexedSeq[Template] = {
    broker.transactional(Database.getConnection()) {
      _.selectAll(
        Template.Tokens.selectByQuery,
        "nodeTypeIds" -> mapToIds(nodeTypes).toArray, "attributeIds" -> mapToIds(attributes).toArray
      )
    }
  }

  object Tokens extends NamedBasicTokens {
    override val selectById = Token('selectTemplateById, extractor)
    override val selectByName = Token('selectTemplateByName, extractor)
    val selectByQuery = Token('selectTemplateByQuery, extractor)
    override val insert = Token('insertTemplate, TemplateRowExtractor)
  }

  /*
  def all[T](t : T*)(implicit m : Manifest[T]) : IndexedSeq[Template] = {
    val clazz = Class.forName(m.toString)
    if (clazz.isAssignableFrom(Attribute.getClass)) {
      broker.transaction(connection) {
        s => s.selectAll(
          Tokens.Template.selectByQuery,
          "attributeIds" -> t.toArray
        )
      }
    } else if (clazz.isAssignableFrom(NodeType.getClass)) {
      broker.transactional(connection) {
        s => s.selectAll(
          Tokens.Template.selectByQuery,
          "nodeTypeIds" -> t.toArray
        )
      }
    } else {
      IndexedSeq.empty[Template]
    }
  }
  */

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

object TemplateRowExtractor extends RowExtractor[Template] {
  val key = Set("template_id")

  def extract(row : Row) = {
    Template(
      row.integer("template_id").get,
      row.integer("node_type_id").get,
      row.string("interface_name").get
    )
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

}