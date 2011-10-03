SELECT  n.node_id, n.node_type_id, n.template_id,
        nam.node_attribute_map_id, nam.value, nam.attribute_id
FROM    nodes as n, node_attributes_map as nam
WHERE   n.node_id = nam.node_id
AND     n.node_id = :id