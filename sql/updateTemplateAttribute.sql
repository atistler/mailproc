UPDATE  template_attributes_map
SET     template_id = :templateAttribute.templateId,
        attribute_id = :templateAttribute.attributeId,
        optional = :templateAttribute.optional,
        default_value = :templateAttribute.value
WHERE   template_attribute_map_id = :templateAttribute.id.get