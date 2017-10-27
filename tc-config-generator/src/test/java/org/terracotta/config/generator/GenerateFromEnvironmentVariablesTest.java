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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.terracotta.config.generator.GenerateFromEnvironmentVariables.*;

public class GenerateFromEnvironmentVariablesTest {

  @Rule
  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void detectSupportedConfigurations_offheap_and_lease_on_cp() throws Exception {
    List<String> list = GenerateFromEnvironmentVariables.detectSupportedConfigurations();
    assertThat(list.size(), is(2));
    assertThat(list, hasItem(LEASE_NAMESPACE));
    assertThat(list, hasItem(OFFHEAP_NAMESPACE));
  }

  @Test
  public void createModelFromMapVariables_all_good() throws Exception {
    List<String> supportedConfigurations = new ArrayList() {{
      add(LEASE_NAMESPACE);
      add(DATAROOTS_NAMESPACE);
      add(OFFHEAP_NAMESPACE);
      add(BACKUP_NAMESPACE);
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");
    envMap.put(PLATFORM_PERSISTENCE, "true");
    envMap.put(OFFHEAP_UNIT, "MB");
    envMap.put("OFFHEAP_RESOURCE_NAME1", "offheap1");
    envMap.put("OFFHEAP_RESOURCE_NAME2", "offheap2");
    envMap.put("OFFHEAP_RESOURCE_SIZE1", "200");
    envMap.put("OFFHEAP_RESOURCE_SIZE2", "4096");
    envMap.put(LEASE_LENGTH, "24");
    envMap.put("DATA_DIRECTORY1", "data1");
    envMap.put("DATA_DIRECTORY2", "data2");

    ConfigurationModel actual = generateFromEnvironmentVariables.createModelFromMapVariables(envMap);
    ConfigurationModel expected = new ConfigurationModel();
    expected.setBackups(true);
    expected.setLeaseLength(24);
    expected.setClientReconnectWindow(150);
    expected.setOffheapUnit("MB");
    expected.setPlatformPersistence(true);

    expected.getOffheapResources().addAll(new ArrayList() {{
      add(new AbstractMap.SimpleEntry<>("offheap1", 200));
      add(new AbstractMap.SimpleEntry<>("offheap2", 4096));
    }});
    expected.getDataDirectories().addAll(new ArrayList() {{
      add("data1");
      add("data2");
    }});
    expected.getServerNames().addAll(new ArrayList() {{
      add("server11");
      add("server12");
    }});

    assertThat(actual, equalTo(expected));

  }



  @Test
  public void createModelFromMapVariables_offheap_missing_unit() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("OFFHEAP_UNIT was not set but is a mandatory variable");

    List<String> supportedConfigurations = new ArrayList() {{
      add(OFFHEAP_NAMESPACE);
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");
    envMap.put("OFFHEAP_RESOURCE_NAME1", "offheap1");
    envMap.put("OFFHEAP_RESOURCE_SIZE1", "200");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }

  @Test
  public void createModelFromMapVariables_offheap_missing_size() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Value for : OFFHEAP_RESOURCE_SIZE1 should be an integer");

    List<String> supportedConfigurations = new ArrayList() {{
      add(OFFHEAP_NAMESPACE);
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");
    envMap.put(OFFHEAP_UNIT, "MB");
    envMap.put("OFFHEAP_RESOURCE_NAME1", "offheap1");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);
  }

  @Test
  public void createModelFromMapVariables_offheap_invalid_size() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Value for : OFFHEAP_RESOURCE_SIZE1 should be > 0");

    List<String> supportedConfigurations = new ArrayList() {{
      add(OFFHEAP_NAMESPACE);
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");
    envMap.put(OFFHEAP_UNIT, "MB");
    envMap.put("OFFHEAP_RESOURCE_NAME1", "offheap1");
    envMap.put("OFFHEAP_RESOURCE_SIZE1", "-50");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }

  @Test
  public void createModelFromMapVariables_invalid_offheaps_sequence() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("OFFHEAP_RESOURCE_NAME environment variables are not numbered with the following pattern : OFFHEAP_RESOURCE_NAME1, OFFHEAP_RESOURCE_NAME2, etc.");

    List<String> supportedConfigurations = new ArrayList() {{
      add(OFFHEAP_NAMESPACE);
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER42", "server12");
    envMap.put(OFFHEAP_UNIT, "MB");
    envMap.put("OFFHEAP_RESOURCE_NAME1", "offheap1");
    envMap.put("OFFHEAP_RESOURCE_SIZE1", "250");
    envMap.put("OFFHEAP_RESOURCE_NAME42", "offheap2");
    envMap.put("OFFHEAP_RESOURCE_SIZE42", "4096");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }

  @Test
  public void createModelFromMapVariables_missing_reconnect_window() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Missing value for : CLIENT_RECONNECT_WINDOW");

    List<String> supportedConfigurations = new ArrayList() {{
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }

  @Test
  public void createModelFromMapVariables_invalid_reconnect_window() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("CLIENT_RECONNECT_WINDOW should be an integer");

    List<String> supportedConfigurations = new ArrayList() {{
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "plif");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }

  @Test
  public void createModelFromMapVariables_invalid_servers_sequence() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("TC_SERVER environment variables are not numbered with the following pattern : TC_SERVER1, TC_SERVER2, etc.");

    List<String> supportedConfigurations = new ArrayList() {{
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER42", "server12");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }

  @Test
  public void createModelFromMapVariables_invalid_lease() throws Exception {

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Value for : LEASE_LENGTH should be an integer");

    List<String> supportedConfigurations = new ArrayList() {{
      add(LEASE_NAMESPACE);
    }};

    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);
    HashMap<String, String> envMap = new HashMap<>();
    envMap.put(CLIENT_RECONNECT_WINDOW, "150");
    envMap.put("TC_SERVER1", "server11");
    envMap.put("TC_SERVER2", "server12");
    envMap.put(LEASE_LENGTH, "plif");

    generateFromEnvironmentVariables.createModelFromMapVariables(envMap);

  }


  @Test
  public void generateXml_ok() throws Exception {

    List<String> supportedConfigurations = new ArrayList() {{
      add(LEASE_NAMESPACE);
      add(DATAROOTS_NAMESPACE);
      add(OFFHEAP_NAMESPACE);
      add(BACKUP_NAMESPACE);
    }};
    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(supportedConfigurations);

    ConfigurationModel configurationModel = new ConfigurationModel();
    configurationModel.setBackups(true);
    configurationModel.setLeaseLength(24);
    configurationModel.setClientReconnectWindow(150);
    configurationModel.setOffheapUnit("MB");
    configurationModel.setPlatformPersistence(true);
    configurationModel.getOffheapResources().addAll(new ArrayList() {{
      add(new AbstractMap.SimpleEntry<>("offheap1", 200));
      add(new AbstractMap.SimpleEntry<>("offheap2", 4096));
    }});
    configurationModel.getDataDirectories().addAll(new ArrayList() {{
      add("data1");
      add("data2");
    }});
    configurationModel.getServerNames().addAll(new ArrayList() {{
      add("server11");
      add("server12");
    }});

    File file = generateFromEnvironmentVariables.generateXml(configurationModel, "./target");

    byte[] expectedEncoded = Files.readAllBytes(Paths.get(this.getClass().getResource("/tc-config-expected").getPath()));
    String expected = new String(expectedEncoded, "UTF-8");

    byte[] actualEncoded = Files.readAllBytes(file.toPath());
    String actual = new String(actualEncoded, "UTF-8");

    assertThat(actual, equalTo(expected));

  }


}