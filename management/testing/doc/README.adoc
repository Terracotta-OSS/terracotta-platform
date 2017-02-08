1. Download Ehcache Clustered Kit:

 http://repo1.maven.org/maven2/org/ehcache/ehcache-clustered/3.2.0/ehcache-clustered-3.2.0-kit.zip

2. Unzip

3. Put mnm-server.jar inside `server/plugins/lib`

4. Put a file named `tc-config.xml` inside `server` folder with this content:

  <?xml version="1.0" encoding="UTF-8"?>
  <tc-config xmlns="http://www.terracotta.org/config"
             xmlns:ohr="http://www.terracotta.org/config/offheap-resource">
    <plugins>
      <config>
        <ohr:offheap-resources>
          <ohr:resource name="primary" unit="MB">64</ohr:resource>
        </ohr:offheap-resources>
      </config>
    </plugins>
  </tc-config>

5. Go to `server` and run: `./bin/start-tc-server.sh -f tc-config.xml`

6. Run any of the examples. For some, you might need to have a cache manager initialized first.
