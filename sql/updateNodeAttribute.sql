UPDATE  node_attributes_map
SET     attribute_id = :nodeAttribute.attributeId,
        value = :nodeAttribute.value
WHERE   node_attribute_map_id = :nodeAttribute.id