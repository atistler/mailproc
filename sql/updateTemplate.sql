UPDATE  templates
SET     node_type_id = :template.nodeTypeId,
        interface_name = :template.name
WHERE   template_id = :template.id.get