UPDATE  connection_types
SET     interface_name = :connectionType.name,
        bidirectional = :connectionType.bidirectional,
        compliment_connection_type_id = :connectionType.complimentTypeId
WHERE   connection_type_id = :connectionType.id.get