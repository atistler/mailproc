SELECT  *
FROM    connections
WHERE   connector_node_id = :connection.connectorId
AND     connectee_node_id = :connection.connecteeId
AND     connection_type_id = :connection.connectionTypeId
LIMIT   1