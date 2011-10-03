UPDATE  node_types
SET     node_type_pool_id = :nodeType.poolId,
        interface_name = :nodeType.name
WHERE   node_type_id = :nodeType.id.get