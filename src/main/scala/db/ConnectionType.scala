package db {

import org.orbroker._

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