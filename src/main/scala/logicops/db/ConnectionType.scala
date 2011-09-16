package logicops.db {

import org.orbroker._

class ConnectionType(
  val id : Option[Int], val name : String, var bidirectional : Boolean = false, val complimentTypeId : Int = 0
  ) extends Dao[ConnectionType] {

  if ( bidirectional ) {
    assert(complimentTypeId > 0)
  }
  override def toString = "ConnectionType[%s] (name: %s)".format(id, name)

  protected val companion = ConnectionType

  def copy(
    id : Option[Int] = this.id, name : String = this.name, bidirectional : Boolean = this.bidirectional,
    complimentTypeId : Int = this.complimentTypeId
    ) = {
    new ConnectionType(id, name, bidirectional, complimentTypeId)
  }

  lazy val complimentType = ConnectionType.get(complimentTypeId)
}


object ConnectionType extends NamedDaoHelper[ConnectionType] {

  val pK = "connection_type_id"
  val tableName = "connection_types"
  val extractor = ConnectionTypeExtractor
  val columnMap = Map("interface_name" -> "name", "compliment_connection_type_id" -> "complimentTypeId")

  def apply(id : Int, name : String, bidirectional : Boolean, complimentTypeId : Int) : ConnectionType = {
    new ConnectionType(Some(id), name, bidirectional, complimentTypeId)
  }

  def apply(id : Int, name : String, bidirectional : Boolean, complimentType : ConnectionType) : ConnectionType = {
    apply(id, name, bidirectional, complimentType.id.get)
  }

  def apply(name : String, bidirectional : Boolean, complimentTypeId : Int) : ConnectionType = {
    new ConnectionType(None, name, bidirectional, complimentTypeId)
  }

  def apply(name : String, bidirectional : Boolean, complimentType : ConnectionType) : ConnectionType = {
    apply(name, bidirectional, complimentType.id.get)
  }

  object Tokens extends NamedBasicTokens {
  }

}

object ConnectionTypeExtractor extends RowExtractor[ConnectionType] {
  val key = Set("connection_type_id")

  def extract(row : Row) = {
    ConnectionType(
      row.integer("connection_type_id").get,
      row.string("interface_name").get,
      row.bit("bidirectional").get,
      row.integer("compliment_connection_type_id").get
    )
  }
}

}