SELECT  *
FROM    connections as c
WHERE   c.connector_node_id = :connector_node_id
AND     c.connection_type_id = :connection_type_id