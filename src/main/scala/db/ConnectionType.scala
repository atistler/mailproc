package db {

import org.orbroker._

class ConnectionType(
  val id : Option[Int], val name : String, var bidirectional : Boolean = false, val complimentTypeId : Int = 0
  ) extends Dao {
  override def toString = "ConnectionType[%s] (name: %s)".format(id, name)

  def copy(
    id : Option[Int] = this.id, name : String = this.name, bidirectional : Boolean = this.bidirectional,
    complimentTypeId : Int = this.complimentTypeId
    ) = {
    new ConnectionType(id, name, bidirectional, complimentTypeId)
  }

  lazy val complimentType = ConnectionType.getMem(complimentTypeId)

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => t.execute(
            Tokens.ConnectionType.update, "connectionType" -> this
          )
          case None => t.executeForKeys(
            Tokens.ConnectionType.insert, "connectionType" -> this
          ) {
            k : Int => copy(id = Some(k))
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
  def apply(name : String, bidirectional : Boolean, complimentTypeId : Int) : ConnectionType = {
    new ConnectionType(None, name, bidirectional, complimentTypeId)
  }

  def apply(name : String, bidirectional : Boolean, complimentType : ConnectionType) : ConnectionType = {
    apply(name, bidirectional, complimentType.id.get)
  }

  def getOption(id : Int) = {
    selectOneOption(Tokens.ConnectionType.selectById, "connection_type_id" -> id)
  }

  def getOption(name : String) = {
    selectOneOption(Tokens.ConnectionType.selectByName, "interface_name" -> name)
  }

  def get(name : String) = {
    getOption(name).get
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

}