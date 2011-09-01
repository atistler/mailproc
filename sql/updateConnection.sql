UPDATE  connections
SET     connection_type_id = :connection.connectionTypeId,
        connector_node_id = :connection.connectorId,
        connector_node_type_id = :connection.connectorTypeId,
        connectee_node_id = :connection.connecteeId,
        connectee_node_type_id = :connection.connecteeTypeId
WHERE   connection_id = :connection.id.get