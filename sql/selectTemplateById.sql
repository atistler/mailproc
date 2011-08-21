SELECT  t.template_id, t.interface_name, t.node_type_id,
        tam.template_attribute_map_id, tam.attribute_id, tam.optional, tam.default_value
FROM    templates as t, template_attributes_map as tam
WHERE   t.template_id = tam.template_id
AND     t.template_id = :template_id