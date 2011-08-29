UPDATE  connection_types
SET     interface_name = :interface_name,  bidirectional = :bidirectional,
        compliment_connection_type_id = :compliment_connection_type_id
WHERE   connection_type_id = :connection_type_id