package logicops.db {

import org.orbroker._

class NodeType(val id : Option[Int], val poolId : Int, val name : String) extends NamedDao[NodeType] {

  override def toString = "NodeType[%s] (name: %s, poolId: %d)".format(id, name, poolId)

  protected val companion = NodeType

  def copy(id : Option[Int] = this.id, poolId : Int = this.poolId, name : String = this.name) = {
    new NodeType(id, poolId, name)
  }
}


object NodeType extends NamedDaoHelper[NodeType] {

  val tableName = "node_types"
  val pK = "node_type_id"
  val extractor = NodeTypeExtractor
  val columnMap = Map("node_type_pool_id" -> "poolId", "interface_name" -> "name")

  def apply(id : Int, poolId : Int, name : String) : NodeType = {
    new NodeType(Some(id), poolId, name)
  }

  def apply(poolId : Int, name : String) : NodeType = {
    new NodeType(None, poolId, name)
  }

  def allByPoolId(poolId : Int) = {
    broker.transactional(Database.getConnection) {
      _.selectAll(Tokens.selectByPoolId, "node_type_pool_id" -> poolId)
    }
  }

  object Tokens extends NamedBasicTokens {
    val selectByPoolId = Token('selectNodeTypesByPoolId, extractor)
  }

}

object NodeTypeExtractor extends RowExtractor[NodeType] {
  val key = Set("node_type_id")

  def extract(row : Row) = {
    NodeType(
      row.integer("node_type_id").get,
      row.integer("node_type_pool_id").get,
      row.string("interface_name").get
    )
  }
}

}