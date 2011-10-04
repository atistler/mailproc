INSERT INTO connections (
    connection_type_id, connector_node_id, connector_node_type_id,
    connectee_node_id, connectee_node_type_id )
VALUES (
    :connection.connectionTypeId, :connection.connectorId, :connection.connectorTypeId,
    :connection.connecteeId, :connection.connecteeTypeId
    )