package db {

import org.orbroker._

class NodeAttribute(
  var id : Option[Int], var nodeId : Int, var attributeId : Int, var value : String
  ) extends Dao {
  override def toString = "NodeAttribute[%s] (nodeId: %d, attribute: %s[%d], value: %s".format(
    id, nodeId, Attribute.getMem(attributeId).name, attributeId, value
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
  def apply(id : Int, node_id : Int, attributeId : Int, value : String) : NodeAttribute = {
    new NodeAttribute(Some(id), node_id, attributeId, value)
  }

  def apply(id : Int, node_id : Int, attribute : Attribute, value : String) : NodeAttribute = {
    NodeAttribute(id, node_id, attribute.id.get, value)
  }

  def apply(id: Int, node : Node, attributeId: Int, value: String) : NodeAttribute = {
    NodeAttribute(id, node.id.get, attributeId, value)
  }

  def apply(id: Int, node : Node, attribute: Attribute, value: String) : NodeAttribute = {
    NodeAttribute(id, node.id.get, attribute.id.get, value)
  }

  def apply(node_id : Int, attributeId : Int, value : String) : NodeAttribute = {
    new NodeAttribute(None, node_id, attributeId, value)
  }

  def apply(node_id : Int, attribute : Attribute, value : String) : NodeAttribute = {
    NodeAttribute(node_id, attribute.id.get, value)
  }

  def apply(node : Node, attributeId: Int, value: String) : NodeAttribute = {
    NodeAttribute(node.id.get, attributeId, value)
  }

  def apply(node : Node, attribute: Attribute, value: String) : NodeAttribute = {
    NodeAttribute(node.id.get, attribute.id.get, value)
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
      row.string("value").getOrElse("")
    )
  }
}

}