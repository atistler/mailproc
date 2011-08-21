SELECT  connection_type_id, interface_name, bidirectional, compliment_connection_type_id
FROM    connection_types
WHERE   connection_type_id = :connection_type_id