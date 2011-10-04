SELECT  t.template_id, t.interface_name, t.node_type_id,
        tam.template_attribute_map_id, tam.attribute_id, tam.optional, tam.default_value
FROM    templates as t, template_attributes_map as tam
WHERE   t.template_id = tam.template_id

<#if (nodeTypeIds?size == 1)>
    AND t.node_type_id = ${nodeTypeIds[0]}
<#elseif (nodeTypeIds?size > 0)>
    AND t.node_type_id IN (
    <#list nodeTypeIds as id>
        <#if (id_index > 0)> , </#if> :nodeTypeIds[${id_index}]
    </#list>
    )
</#if>

<#if (attributeIds?size == 1)>
    AND tam.attribute_id = ${attributeIds[0]}
<#elseif (attributeIds?size > 0)>
    AND tam.attribute_id IN (
    <#list attributeIds as id>
        <#if (id_index > 0)> , </#if> :attributeIds[${id_index}]
    </#list>
    )
</#if>

