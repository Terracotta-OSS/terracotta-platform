#TC Config Generator

This module can generate a tc-config file from a bunch of environment variables.

## Example
For example, given the following environment variables :
```
export TCCONFIG_SERVERS0_NAME=server11
export TCCONFIG_SERVERS0_HOST=server11
export TCCONFIG_SERVERS0_PORT=9410
export TCCONFIG_SERVERS0_GROUP_PORT=9430
export TCCONFIG_SERVERS1_NAME=server12
export TCCONFIG_SERVERS1_HOST=server12
export TCCONFIG_SERVERS1_PORT=9410
export TCCONFIG_SERVERS1_GROUP_PORT=9430
export TCCONFIG_OFFHEAPS0_NAME=offheap1
export TCCONFIG_OFFHEAPS0_UNIT=MB
export TCCONFIG_OFFHEAPS0_VALUE=512
export TCCONFIG_CLIENT_RECONNECT_WINDOW=150
```

and template :
```
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
      <tsa-port>${server.tsaPort}</tsa-port>
      <tsa-group-port>${server.tsaGroupPort}</tsa-group-port>
    </server>
</#list>
    <client-reconnect-window>${clientReconnectWindow}</client-reconnect-window>
  </servers>
</tc-config>
```

You can run the generator with :

```
java -jar tc-config-generator -i /Users/anthony/tc-template.ftlh -o /Users/anthony/tc-config.xml
```

and end up with a tc-config-generated.xml file, with the following content :

```xml
<?xml version="1.0" encoding="UTF-8"?>

<tc-config xmlns="http://www.terracotta.org/config">
  <plugins>
    <config>
      <ohr:offheap-resources xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
        <ohr:resource name="offheap1" unit="MB">200</ohr:resource>
      </ohr:offheap-resources>
    </config>
  </plugins>
  <servers>
    <server host="server11" name="server11">
      <tsa-port>9410</tsa-port>
      <tsa-group-port>9430</tsa-group-port>
    </server>
    <server host="server12" name="server12">
      <tsa-port>9410</tsa-port>
      <tsa-group-port>9430</tsa-group-port>
    </server>
    <client-reconnect-window>150</client-reconnect-window>
  </servers>
</tc-config>
```

## How does that work ?

The tc-config-generator is based on a template engine (Apache Freemarker); it then allows you to provide any Freemarker based template.

When you start it, it will parse environment variables (using the TCCONFIG_ prefix) and convert them into camel-case variables before feeding the template engine.
In the previous example, TCCONFIG_CLIENT_RECONNECT_WINDOW was fed into the template engine as clientReconnectWindow.

Also, lists are supported as well, using indexed environment variables : TCCONFIG_SERVERS0_HOST indicates that a list named "servers" with a map containing a key named "host" shall be fed to the template engine.