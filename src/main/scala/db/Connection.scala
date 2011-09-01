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
  val id : Option[Int], val connectionTypeId : Int, val connectorId : Int, val connectorTypeId : Int,
  val connecteeId : Int, val connecteeTypeId : Int
  ) extends Dao {
  override def toString = "Connection[%s] (type: %s[%d], connector: %d (%s[%d]), connectee: %d (%s[%d])".format(
    id, ConnectionType.getMem(connectionTypeId), connectionTypeId, connectorId, NodeType.getMem(connectorTypeId).name,
    connectorTypeId, connecteeId,
    NodeType.getMem(connecteeTypeId), connecteeTypeId
  )

  def copy(
    id : Option[Int] = this.id, connectionTypeId : Int = this.connectionTypeId, connectorId : Int = this.connectorId,
    connectorTypeId : Int = this.connectorTypeId, connecteeId : Int = this.connecteeId,
    connecteeTypeId : Int = this.connecteeTypeId
    ) = {
    new Connection(id, connectionTypeId, connectorId, connectorTypeId, connecteeId, connecteeTypeId)
  }

  lazy val connectionType = ConnectionType.getMem(connectionTypeId)
  lazy val connectee = Node.get(connecteeId)
  lazy val connecteeType = NodeType.getMem(connecteeTypeId)
  lazy val connector = Node.get(connectorId)
  lazy val connectorType = NodeType.getMem(connectorTypeId)

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => throw new DaoException("Connections cannot be updated: " + this)
          case None => t.executeForKeys(
            Tokens.Connection.insert, "connection" -> this
          ) {
            k : Int => copy(id = Some(k))
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
      connectionId, connectionType.id.get, connecteeId, connecteeType.id.get, connectorId,
      connectorType.id.get
    )
  }

  def apply(
    connectionId : Int, connectionType : String, connecteeId : Int, connecteeType : String,
    connectorId : Int, connectorType : String
    ) : Connection = {
    apply(
      connectionId, ConnectionType.get(connectionType), connecteeId, NodeType.get(connecteeType),
      connectorId, NodeType.get(connectorType)
    )
  }

  def apply(
    connectionId : Int, connectionTypeId : Int, connectee : Node, connector : Node
    ) : Connection = {
    apply(
      connectionId, connectionTypeId, connectee.id.get,
      connectee.nodeTypeId,
      connector.id.get,
      connector.nodeTypeId
    )
  }

  def apply(
    connectionId : Int, connectionType : ConnectionType, connectee : Node, connector : Node
    ) : Connection = {
    apply(
      connectionId, connectionType.id.get, connectee.id.get, connectee.nodeTypeId,
      connector.id.get,
      connector.nodeTypeId
    )
  }

  def apply(
    connectionId : Int, connectionType : String, connectee : Node, connector : Node
    ) : Connection = {
    apply(
      connectionId, ConnectionType.get(connectionType), connectee.id.get, connectee.nodeTypeId,
      connector.id.get,
      connector.nodeTypeId
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
    connectorId : Int, connectorType : NodeType
    ) : Connection = {
    apply(
      connectionType.id.get, connecteeId, connecteeType.id.get,
      connectorId,
      connectorType.id.get
    )
  }

  def apply(
    connectionType : String, connecteeId : Int, connecteeType : String,
    connectorId : Int, connectorType : String
    ) : Connection = {
    apply(
      ConnectionType.get(connectionType), connecteeId, NodeType.get(connecteeType),
      connectorId, NodeType.get(connectorType)
    )
  }

  def apply(
    connectionTypeId : Int, connectee : Node, connector : Node
    ) : Connection = {
    apply(
      connectionTypeId, connectee.id.get, connectee.nodeTypeId,
      connector.id.get, connector.nodeTypeId
    )
  }

  def apply(connectionType : ConnectionType, connectee : Node, connector : Node) : Connection = {
    apply(
      connectionType.id.get, connectee, connector
    )
  }

  def apply(connectionType : String, connectee : Node, connector : Node) : Connection = {
    apply(
      ConnectionType.get(connectionType), connectee, connector
    )
  }

  def getOption(id : Int) = {
    broker.transactional(connection) {
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