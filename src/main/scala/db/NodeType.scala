package db {

import org.orbroker._

class NodeType(val id : Option[Int], val poolId : Int, val name : String) extends Dao {

  override def toString = "NodeType[%s] (name: %s, poolId: %d)".format(id, name, poolId)

  def copy(id : Option[Int] = this.id, poolId : Int = this.poolId, name : String = this.name) = {
    new NodeType(id, poolId, name)
  }

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          /* TODO: NodeTypes should really never by updated ( maybe just interface_name ) */
          case Some(_id) => t.execute(
            Tokens.NodeType.update, "nodeType" -> this
          )
          case None => t.executeForKeys(Tokens.NodeType.insert, "nodeType" -> this) {
            k : Int => copy(id = Some(k))
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
    broker.transactional(connection) {
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