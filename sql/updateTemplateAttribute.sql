UPDATE  template_attributes_map
SET     template_id = :template_id, attribute_id = :attribute_id, optional = :optional, default_value = :default_value
WHERE   template_attribute_map_id = :template_attribute_map_id