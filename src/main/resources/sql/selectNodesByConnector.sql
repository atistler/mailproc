SELECT  distinct(n.node_id), n.node_type_id, n.template_id,
        nam.node_attribute_map_id, nam.value, nam.attribute_id
FROM    nodes as n, connections as c, node_attributes_map as nam
WHERE   n.node_id = c.connectee_node_id
AND     n.node_id = nam.node_id


<#if connectionTypes?? && (connectionTypes?size == 1)>
    AND c.connection_type_id = :connectionTypes[0]
<#elseif connectionType?? && (connectionType?size > 0)>
    AND c.connection_type_id IN (
    <#list connectionTypes as connectionType>
        <#if (connectionType_index > 0)> , </#if> :connectionType
    </#list>
    )
</#if>

<#if connecteeTypes?? && (connecteeTypes?size == 1)>
    AND c.connectee_node_type_id = :connecteeTypes[0]
<#elseif connecteeTypes?? && (connecteeTypes?size > 0)>
    AND c.connectee_node_type_id IN (
    <#list connecteeTypes as connecteeType>
        <#if (connecteeType_index > 0)> , </#if> :connecteeType
    </#list>
    )
</#if>

<#if node??>
    AND c.connector_node_id = :node.id
<#elseif nodes?? && (nodes?size == 1)>
    AND c.connector_node_id = :nodes[0]
<#elseif nodes?? && (nodes?size > 0)>
    AND c.connector_node_id IN (
    <#list nodes as nodeid>
        <#if (nodeid_index > 0)> , </#if> ${nodeid}
    </#list>
    )
</#if>

ORDER BY n.node_id, nam.attribute_id