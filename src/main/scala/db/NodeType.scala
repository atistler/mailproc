package db {

import org.orbroker._

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

}