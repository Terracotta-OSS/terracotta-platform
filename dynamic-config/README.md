# Dynamic Configuration

This is a repository of useful docs and command for the project.

## Build KIT

```
# Build KIT
> ./gradlew clean assemble
```

## Build KIT with TMC

Install node and npm, then, for the first time:

```
# Fix Node to an old version because the dependencies used in TMC are so old or unsupported now that we cannot use the new version
> npm install -g n
> n 11.15.0

# Install gulp
> npm install -g gulp-cli

# Fetch deps 
> cd management/tmc
> npm install .

# Verify TMC build works
> gulp
```

To build a kit with TMC webapp inside:

```
# Build KIT
> ./gradlew clean assemble -Ptmc
```

## Dynamic Config M&M notification demo

```
# Unzip KIT
> unzip -d ./distribution/kit/build/distributions/ ./distribution/kit/build/distributions/terracotta-10.7.0-SNAPSHOT.zip
```

Start a cluster:

```
# Start a configured node in its own cluster
> ./distribution/kit/build/distributions/terracotta-10.7.0-SNAPSHOT/server/bin/start-node.sh \
    --node-hostname=localhost \
    --node-port=9410 \
    --cluster-name=my-cluster \
    --license-file=./dynamic-config/integration-tests/src/test/resources/license.xml
```

Run the small main class that uses no TMS but just the management API to get the notifications:

```
# Run the main class: ReadNotifications
# This connects to the NMS entity and listens for incoming notifications
```

Run the TMS:

```
# Run TMS
> TMS_AUTOCONNECT=true TMS_DEFAULTURL=terracotta://localhost:9410 ./distribution/kit/build/distributions/terracotta-10.7.0-SNAPSHOT/tools/management/bin/start.sh

# Open TMC: http://localhost:9480/index.html
# Go to to the event log panel 
```

Do an offheap addition at runtime:
- We should see from the ReadNotifications program the notifications: DYNAMIC_CONFIG_SAVED and DYNAMIC_CONFIG_SET
- We should see 2 events in the TMC

```
# This will add the offheap resource "second"
> ./distribution/kit/build/distributions/terracotta-10.7.0-SNAPSHOT/tools/config-tool/bin/config-tool.sh set -s localhost -c offheap-resources.second=512MB
```



Do a change that requires a restart:
- We should see from the ReadNotifications program the notifications: DYNAMIC_CONFIG_SAVED and DYNAMIC_CONFIG_SET
- We should see 2 events in the TMC, and one should be of type alert to tell the user it needs a restart

```
# This will set "foo.bar=blah" in the <tc-properties> section of the xml file
> ./distribution/kit/build/distributions/terracotta-10.7.0-SNAPSHOT/tools/config-tool/bin/config-tool.sh set -s localhost -c stripe.1.node.1.tc-properties.foo.bar=blah
```
