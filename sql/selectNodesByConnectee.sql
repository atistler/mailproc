SELECT  n.node_id as node_id, n.node_type_id as node_type_id, n.template_id,
        nam.node_attribute_map_id, nam.value, nam.attribute_id
FROM    nodes as n, connections as c, node_attributes_map as nam, templates as t
WHERE   n.node_id = c.connector_node_id
AND     n.node_id = nam.node_id

<#if connectionTypeIds?? && (connectionTypeIds?size == 1)>
    AND c.connection_type_id = ${connectionTypeIds[0]}
<#elseif connectionTypeIds?? && (connectionTypeIds?size > 0)>
    AND c.connection_type_id IN (
    <#list connectionTypeIds as id>
        <#if (id_index > 0)> , </#if> :connectionTypeIds[${id_index}]
    </#list>
    )
</#if>

<#if connectorTypeIds?? && (connectorTypeIds?size == 1)>
    AND c.connector_node_type_id = ${connectorTypeIds[0]}
<#elseif connectorTypeIds?? && (connectorTypeIds?size > 0)>
    AND c.connector_node_type_id IN (
    <#list connectorTypeIds as id>
        <#if (id_index > 0)> , </#if> :connectorTypeIds[${id_index}]
    </#list>
    )
</#if>

<#if connecteeTypeIds?? && (connecteeTypeIds?size == 1)>
    AND c.connectee_node_type_id = ${connecteeTypeIds[0]}
<#elseif connecteeTypeIds?? && (connecteeTypeIds?size > 0)>
    AND c.connectee_node_type_id IN (
    <#list connecteeTypeIds as id>
        <#if (id_index > 0)> , </#if> :connecteeTypeIds[${id_index}]
    </#list>
    )
</#if>

<#if connecteeId??>
    AND c.connectee_node_id = :connecteeId
<#elseif connecteeIds?? && (connecteeIds?size == 1)>
    AND c.connectee_node_id = ${connecteeIds[0]}
<#elseif connecteeIds?? && (connecteeIds?size > 0)>
    AND c.connectee_node_id IN (
    <#list connecteeIds as id>
        <#if (id_index > 0)> , </#if> :connecteeIds[${id_index}]
    </#list>
    )
</#if>