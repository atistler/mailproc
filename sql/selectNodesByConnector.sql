SELECT  distinct(n.node_id), n.node_type_id, n.template_id,
        nam.node_attribute_map_id, nam.value, nam.attribute_id
FROM    nodes as n, connections as c, node_attributes_map as nam

<#if attributeIds?? && (attributeIds?size > 0)>
    <#list attributeIds as attributeId>
        , node_attributes_map as nam${attributeId_index}
    </#list>
</#if>

WHERE   n.node_id = c.connectee_node_id
AND     n.node_id = nam.node_id

<#if attributeIds?? && (attributeIds?size > 0)>
    <#list attributeIds as attributeId>
        AND nam${attributeId_index}.node_id = c.connectee_node_id
    </#list>
</#if>

<#if attributeIds?? && (attributeIds?size > 0)>
    <#list attributeIds as attributeId>
        AND nam${attributeId_index}.attribute_id = :attributeIds[${attributeId_index}]

    </#list>
</#if>

<#if attributeValues?? && (attributeValues?size > 0)>
    <#list attributeValues as attributeValue>
        AND nam${attributeValue_index}.value = :attributeValues[${attributeValue_index}]
    </#list>
</#if>


<#if connectionTypes?? && (connectionTypes?size == 1)>
    AND c.connection_type_id = :connectionTypes[0]
<#elseif connectionTypes?? && (connectionTypes?size > 1)>
    AND c.connection_type_id IN (
    <#list connectionTypes as connectionType>
        <#if (connectionType_index > 0)> , </#if> :connectionTypes[${connectionType_index}]
    </#list>
    )
</#if>

<#if connecteeTypes?? && (connecteeTypes?size == 1)>
    AND c.connectee_node_type_id = :connecteeTypes[0]
<#elseif connecteeTypes?? && (connecteeTypes?size > 1)>
    AND c.connectee_node_type_id IN (
    <#list connecteeTypes as connecteeType>
        <#if (connecteeType_index > 0)> , </#if> :connecteeTypes[${connecteeType_index}]
    </#list>
    )
</#if>

<#if node??>
    AND c.connector_node_id = :node.id
<#elseif nodes?? && (nodes?size == 1)>
    AND c.connector_node_id = :nodes[0]
<#elseif nodes?? && (nodes?size > 1)>
    AND c.connector_node_id IN (
    <#list nodes as nodeid>
        <#if (nodeid_index > 0)>, </#if> :nodes[${nodeid_index}]
    </#list>
    )
</#if>

ORDER BY n.node_id, nam.attribute_id