/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.config.generator;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class GenerateFromEnvironmentVariablesTest {

  private static Configuration createTemplateConfiguration() throws IOException {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
    configuration.setTemplateLoader(new ClassTemplateLoader(GenerateFromEnvironmentVariablesTest.class, "/"));
    configuration.setDefaultEncoding("UTF-8");
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    configuration.setLogTemplateExceptions(false);
    return configuration;
  }

  @Rule
  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void createModelFromMapVariables_all_good() throws Exception {

    Map<String, String> map =  new HashMap<>();
    map.put("TCCONFIG_SERVERS0_NAME", "server11");
    map.put("TCCONFIG_SERVERS0_HOST", "server11");
    map.put("TCCONFIG_SERVERS0_PORT", "9410");
    map.put("TCCONFIG_SERVERS0_GROUP_PORT", "9430");
    map.put("TCCONFIG_SERVERS1_NAME", "server12");
    map.put("TCCONFIG_SERVERS1_HOST", "server12");
    map.put("TCCONFIG_SERVERS1_PORT", "9410");
    map.put("TCCONFIG_SERVERS1_GROUP_PORT", "9430");
    map.put("TCCONFIG_OFFHEAPS0_NAME", "offheap1");
    map.put("TCCONFIG_OFFHEAPS0_UNIT", "MB");
    map.put("TCCONFIG_OFFHEAPS0_VALUE", "512");
    map.put("TCCONFIG_OFFHEAPS1_NAME", "offheap2");
    map.put("TCCONFIG_OFFHEAPS1_UNIT", "GB");
    map.put("TCCONFIG_OFFHEAPS1_VALUE", "10");
    map.put("TCCONFIG_CLIENT_RECONNECT_WINDOW", "150");

    Configuration configuration = createTemplateConfiguration();
    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(configuration);
    Map<String, Object> root = generateFromEnvironmentVariables.retrieveNamesWithPrefix(map, GenerateFromEnvironmentVariables.TCCONFIG_);

    Map expected = new TreeMap();
    expected.put("servers", new ArrayList(){{
      add(new TreeMap() {{
        put("name", "server11");
        put("host", "server11");
        put("port", "9410");
        put("groupPort", "9430");
      }});
      add(new TreeMap() {{
        put("name", "server12");
        put("host", "server12");
        put("port", "9410");
        put("groupPort", "9430");
      }});
    }});
    expected.put("offheaps", new ArrayList(){{
      add(new TreeMap() {{
        put("name", "offheap1");
        put("unit", "MB");
        put("value", "512");
      }});
      add(new TreeMap() {{
        put("name", "offheap2");
        put("unit", "GB");
        put("value", "10");
      }});
    }});
    expected.put("clientReconnectWindow", "150");
    assertThat(root, equalTo(expected));

  }

  @Test
  public void generateXml_ok() throws Exception {
    Map root = new TreeMap();
    root.put("servers", new ArrayList(){{
      add(new TreeMap() {{
        put("name", "server11");
        put("host", "server11");
        put("port", "9410");
        put("groupPort", "9430");
      }});
      add(new TreeMap() {{
        put("name", "server12");
        put("host", "server12");
        put("port", "9410");
        put("groupPort", "9430");
      }});
    }});
    root.put("offheaps", new ArrayList(){{
      add(new TreeMap() {{
        put("name", "offheap1");
        put("unit", "MB");
        put("value", "512");
      }});
      add(new TreeMap() {{
        put("name", "offheap2");
        put("unit", "GB");
        put("value", "10");
      }});
    }});
    root.put("clientReconnectWindow", "150");
    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(createTemplateConfiguration());
    StringWriter output = new StringWriter();
    generateFromEnvironmentVariables.generateXmlFile(root, "template-tc-config.ftlh", output);

    byte[] expectedEncoded = Files.readAllBytes(Paths.get(this.getClass().getResource("/tc-config-expected").getPath()));
    String expected = new String(expectedEncoded, "UTF-8");
    String actual = output.toString();
    assertThat(actual, equalTo(expected));

  }

}