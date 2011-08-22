SELECT  *
FROM    connections as c
WHERE   c.connectee_node_id = :connectee_node_id
AND     c.connection_type_id = :connection_type_id