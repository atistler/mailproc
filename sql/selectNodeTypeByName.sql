SELECT  node_type_id, node_type_pool_id, interface_name
FROM    node_types
WHERE   interface_name = :interface_name