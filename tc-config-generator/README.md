#TC Config Generator

This module can generate a tc-config file from a bunch of environment variables.

## Example
For example, given the following environment variables :
```
CLIENT_RECONNECT_WINDOW=150
TC_SERVER1=server11
TC_SERVER2=server12
PLATFORM_PERSISTENCE=true
OFFHEAP_UNIT=MB
OFFHEAP_RESOURCE_NAME1=offheap1
OFFHEAP_RESOURCE_SIZE1=150
DATA_DIRECTORY1=data1
DATA_DIRECTORY2=data2
LEASE_LENGTH=24
```

You can run the generator with :

```
java -jar target/tc-config-generator-5.4-SNAPSHOT.jar
```

and end up with a tc-config-generated.xml file, with the following content :

```xml
<?xml version="1.0" encoding="UTF-8"?>

<tc-config xmlns="http://www.terracotta.org/config">
  <plugins>

    <service>
      <lease:connection-leasing xmlns:lease="http://www.terracotta.org/service/lease">
        <lease:lease-length unit="seconds">24</lease:lease-length>
      </lease:connection-leasing>
    </service>

    <config>
      <ohr:offheap-resources xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
        <ohr:resource name="offheap1" unit="MB">200</ohr:resource>
        <ohr:resource name="offheap2" unit="MB">4096</ohr:resource>
      </ohr:offheap-resources>
    </config>

    <config>
      <data:data-directories xmlns:data="http://www.terracottatech.com/config/data-roots">
        <data:directory name="platform" use-for-platform="true">/data/data-directories/platform</data:directory>
        <data:directory name="data1" use-for-platform="false">/data/data-directories/data1</data:directory>
        <data:directory name="data2" use-for-platform="false">/data/data-directories/data2</data:directory>
      </data:data-directories>
    </config>

    <service>
      <backup:backup-restore xmlns:backup="http://www.terracottatech.com/config/backup-restore">
        <backup:backup-location path="/data/backups/"/>
      </backup:backup-restore>
    </service>

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

## Supported environment variables

* CLIENT_RECONNECT_WINDOW : an integer value to define how long, in seconds,  the server should wait before closing the client connection window
* TC_SERVERXX : a server name (should also be a valid hostname), XX being a number incremented from 1 to n (n being the number of servers in the stripe)
* PLATFORM_PERSISTENCE : an optional variable to generate, if data root persistence is available, such a plugin configuration :
```xml
<data:directory name="platform" use-for-platform="true">/data/data-directories/platform</data:directory>
```
It's worth noticing the path is hardcoded to /data/data-directories/platform
* DATA_DIRECTORYXX :an optional variable to define a data directory name, XX being a number incremented from 1 to n(n being the number of data directories)
```xml
<data:directory name="data1" use-for-platform="false">/data/data-directories/DATA_DIRECTORY_XX</data:directory>
```
It's worth noticing the path is hardcoded to /data/data-directories/DATA_DIRECTORY_XX
* LEASE_LENGTH : an optional variable to define the duration, in seconds,  of the client connection lease
```xml
<service>
  <lease:connection-leasing xmlns:lease="http://www.terracotta.org/service/lease">
    <lease:lease-length unit="seconds">24</lease:lease-length>
  </lease:connection-leasing>
</service>
```
* OFFHEAP_UNIT : an optional variable to choose the unit of offheap resources
* OFFHEAP_RESOURCE_NAMEXX :an optional variable to define an offheap resource name, XX being a number incremented from 1 to n(n being the number of offheap resources)
* OFFHEAP_RESOURCE_SIZEXX :an optional variable to define an offheap resource size, XX being a number incremented from 1 to n(n being the number of offheap resources); OFFHEAP_RESOURCE_SIZEXX must match OFFHEAP_RESOURCE_NAMEXX
```xml
<config>
  <ohr:offheap-resources xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
    <ohr:resource name="offheap1" unit="MB">200</ohr:resource>
    <ohr:resource name="offheap2" unit="MB">4096</ohr:resource>
  </ohr:offheap-resources>
</config>
```