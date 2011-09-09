UPDATE  attributes
SET     interface_name = :attribute.name,
        openended = :attribute.openended
WHERE   attribute_id = :attribute.id