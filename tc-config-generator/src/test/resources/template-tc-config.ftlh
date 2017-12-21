<?xml version="1.0" encoding="UTF-8"?>

<tc-config xmlns="http://www.terracotta.org/config">
  <plugins>
<#if offheaps??>
    <config>
      <ohr:offheap-resources xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
<#list offheaps as offheap>
        <ohr:resource name="${offheap.name}" unit="${offheap.unit}">${offheap.value}</ohr:resource>
</#list>
      </ohr:offheap-resources>
    </config>
</#if>
  </plugins>
  <servers>
<#list servers as server>
    <server host="${server.host}" name="${server.name}">
      <tsa-port>${server.port}</tsa-port>
      <tsa-group-port>${server.groupPort}</tsa-group-port>
    </server>
</#list>
    <client-reconnect-window>${clientReconnectWindow}</client-reconnect-window>
  </servers>
</tc-config>