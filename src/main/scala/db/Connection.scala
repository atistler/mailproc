package db {

import org.orbroker._

class ConnectionSeq(iseq : IndexedSeq[Connection]) {
  def nodes() {
    iseq foreach {
      c => println(c)
    }
  }
}

class Connection(
  var id : Option[Int], var connectionTypeId : Int, var connectorId : Int, var connectorTypeId : Int,
  var connecteeId : Int, var connecteeTypeId : Int
  ) extends Dao {
  override def toString = "Connection[%s] (type: %s[%d], connector: %d (%s[%d]), connectee: %d (%s[%d])".format(
    id, ConnectionType.getMem(connectionTypeId), connectionTypeId, connectorId, NodeType.getMem(connectorTypeId).name,
    connectorTypeId, connecteeId,
    NodeType.getMem(connecteeTypeId), connecteeTypeId
  )
  lazy val connectionType = ConnectionType.getMem(connectionTypeId)
  lazy val connectee = Node.get(connecteeId)
  lazy val connecteeType = NodeType.getMem(connecteeTypeId)
  lazy val connector = Node.get(connectorId)
  lazy val connectorType = NodeType.getMem(connectorTypeId)

  def save() = {
    broker.transaction() {
      t =>
        id match {
          case Some(_id) => throw new DaoException("Connections cannot be updated: " + this)
          case None => t.callForKeys(
            Tokens.Connection.insert, "connection_type_id" -> connectionType.id.get,
            "connector_node_id" -> connectorId,
            "connector_node_type_id" -> connectorType.id.get, "connectee_node_id" -> connecteeId,
            "connectee_node_type_id" -> connecteeType.id.get
          ) {
            k : Int => id = Some(k)
          }
        }
    }
  }
}

object Connection extends DaoHelper[Connection] {
  def apply(
    connectionId : Int, connectionTypeId : Int, connecteeId : Int, connecteeTypeId : Int,
    connectorId : Int,
    connectorTypeId : Int
    ) : Connection = {
    new Connection(
      Some(connectionId), connectionTypeId, connecteeId,
      connecteeTypeId,
      connectorId,
      connectorTypeId
    )
  }

  def apply(
    connectionId : Int, connectionType : ConnectionType, connecteeId : Int, connecteeType : NodeType,
    connectorId : Int,
    connectorType : NodeType
    ) : Connection = {
    apply(
      connectionId, connectionType.id.get, connecteeId, connecteeType.id.get,
      connectorId,
      connectorType.id.get
    )
  }

  def apply(
    connectionId : Int, connectionTypeId : Int, connectee : Node, connecteeTypeId : Int,
    connector : Node,
    connectorTypeId : Int
    ) : Connection = {
    apply(
      connectionId, connectionTypeId, connectee.id.get,
      connecteeTypeId,
      connector.id.get,
      connectorTypeId
    )
  }

  def apply(
    connectionId : Int, connectionType : ConnectionType, connectee : Node, connecteeType : NodeType,
    connector : Node,
    connectorType : NodeType
    ) : Connection = {
    apply(
      connectionId, connectionType.id.get, connectee.id.get, connecteeType.id.get,
      connector.id.get,
      connectorType.id.get
    )
  }

  def apply(
    connectionTypeId : Int, connecteeId : Int, connecteeTypeId : Int, connectorId : Int,
    connectorTypeId : Int
    ) : Connection = {
    new Connection(
      None, connectionTypeId, connecteeId, connecteeTypeId,
      connectorId,
      connectorTypeId
    )
  }

  def apply(
    connectionType : ConnectionType, connecteeId : Int, connecteeType : NodeType,
    connectorId : Int,
    connectorType : NodeType
    ) : Connection = {
    apply(
      connectionType.id.get, connecteeId, connecteeType.id.get,
      connectorId,
      connectorType.id.get
    )
  }

  def apply(
    connectionTypeId : Int, connectee : Node, connecteeTypeId : Int, connector : Node,
    connectorTypeId : Int
    ) : Connection = {
    new Connection(
      None, connectionTypeId, connectee.id.get, connecteeTypeId,
      connector.id.get,
      connectorTypeId
    )
  }

  def apply(
    connectionType : ConnectionType, connectee : Node, connecteeType : NodeType,
    connector : Node,
    connectorType : NodeType
    ) : Connection = {
    apply(
      connectionType.id.get, connectee.id.get, connecteeType.id.get,
      connector.id.get,
      connectorType.id.get
    )
  }

  def getOption(id : Int) = {
    broker.readOnly() {
      s =>
        s.selectOne(Tokens.Connection.selectById, "connection_id" -> id)
    }
  }
}

object ConnectionExtractor extends JoinExtractor[Connection] {
  val key = Set("connection_id")

  def extract(row : Row, join : Join) = {
    Connection(
      row.integer("connection_id").get,
      row.integer("connection_type_id").get,
      row.integer("connector_node_id").get,
      row.integer("connector_node_type_id").get,
      row.integer("connectee_node_id").get,
      row.integer("connectee_node_type_id").get
    )
  }
}

}