SELECT  ct.connection_type_id, ct.interface_name as connection_type_name, ct.bidirectional, ct.compliment_connection_type_id,
        c.connection_id,
        c.connector_node_id, c.connector_node_type_id, nt1.interface_name as connector_node_type_name, nt1.node_type_pool_id as connector_node_type_pool_id,
        c.connectee_node_id, c.connectee_node_type_id, nt2.interface_name as connectee_node_type_name, nt2.node_type_pool_id as connectee_node_type_pool_id
FROM    connections as c, connection_types as ct, node_types as nt1, node_types as nt2
WHERE   c.connection_type_id = ct.connection_type_id
AND     c.connector_node_type_id = nt1.node_type_id
AND     c.connectee_node_type_id = nt2.node_type_id
AND     c.connectee_node_id = :connectee_node_id