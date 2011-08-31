package db {

import org.orbroker._
import scala.collection.mutable.HashMap

class Template(var id : Option[Int], var nodeTypeId : Int, var name : String) extends Dao {
  private[db] val attributes = new HashMap[String, TemplateAttribute]()

  override def toString = "Template[%s] (name: %s, nodeType: %s[%d])".format(id, name, nodeType.name, nodeTypeId)

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
 /*
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