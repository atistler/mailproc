SELECT  node_attribute_map_id, node_id, attribute_id, value, timestamp
FROM    node_attributes_map
WHERE   node_attribute_map_id = :node_attribute_map_id