SELECT  connection_type_id, interface_name, bidirectional, compliment_connection_type_id
FROM    connection_types
WHERE   interface_name = :interface_name