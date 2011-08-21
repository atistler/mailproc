SELECT  n.node_id, n.node_type_id, n.template_id,
        nam.node_attribute_map_id, nam.value, nam.attribute_id,
        nt.node_type_pool_id, nt.interface_name
FROM    nodes as n, node_attributes_map as nam, node_types as nt
WHERE   n.node_id = nam.node_id
AND     n.node_type_id = nt.node_type_id
AND     n.node_id = :node_id