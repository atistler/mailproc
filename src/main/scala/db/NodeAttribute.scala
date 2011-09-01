package db {

import org.orbroker._

class NodeAttribute(
  val id : Option[Int], val nodeId : Int, val attributeId : Int, val value : String
  ) extends Dao {

  override def toString = "NodeAttribute[%s] (nodeId: %d, attribute: %s[%d], value: %s".format(
    id, nodeId, Attribute.getMem(attributeId).name, attributeId, value
  )

  def copy(
    id : Option[Int] = this.id, nodeId : Int = this.nodeId, attributeId : Int = this.attributeId,
    value : String = this.value
    ) = {
    new NodeAttribute(id, nodeId, attributeId, value)
  }

  lazy val attribute = Attribute.getMem(attributeId)

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => t.execute(
            Tokens.NodeAttribute.update, "nodeAttribute" -> this
          )
          case None => t.executeForKeys(
            Tokens.NodeAttribute.insert, "nodeAttribute" -> this
          ) {
            k : Int => copy(id = Some(k))
          }
        }
    }
  }
}

object NodeAttribute extends DaoHelper[NodeAttribute] {
  def apply(id : Int, nodeId : Int, attributeId : Int, value : String) : NodeAttribute = {
    new NodeAttribute(Some(id), node_id, attributeId, value)
  }

  def apply(id : Int, nodeId : Int, attribute : Attribute, value : String) : NodeAttribute = {
    apply(id, node_id, attribute.id.get, value)
  }

  def apply(id : Int, nodeId : Int, attribute : String, value : String) : NodeAttribute = {
    apply(id, node_id, Attribute.get(attribute), value)
  }

  def apply(id : Int, node : Node, attributeId : Int, value : String) : NodeAttribute = {
    apply(id, node.id.get, attributeId, value)
  }

  def apply(id : Int, node : Node, attribute : Attribute, value : String) : NodeAttribute = {
    apply(id, node.id.get, attribute.id.get, value)
  }

  def apply(id : Int, node : Node, attribute : String, value : String) : NodeAttribute = {
    apply(id, node, Attribute.get(attribute), value)
  }

  def apply(nodeId : Int, attributeId : Int, value : String) : NodeAttribute = {
    new NodeAttribute(None, nodeId, attributeId, value)
  }

  def apply(nodeId : Int, attribute : Attribute, value : String) : NodeAttribute = {
    apply(nodeId, attribute.id.get, value)
  }

  def apply(nodeId : Int, attribute : String, value : String) : NodeAttribute = {
    apply(nodeId, Attribute.get(attribute), value)
  }

  def apply(node : Node, attributeId : Int, value : String) : NodeAttribute = {
    apply(node.id.get, attributeId, value)
  }

  def apply(node : Node, attribute : Attribute, value : String) : NodeAttribute = {
    apply(node.id.get, attribute.id.get, value)
  }

  def apply(node : Node, attribute : String, value : String) : NodeAttribute = {
    apply(node, Attribute.get(attribute), value)
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