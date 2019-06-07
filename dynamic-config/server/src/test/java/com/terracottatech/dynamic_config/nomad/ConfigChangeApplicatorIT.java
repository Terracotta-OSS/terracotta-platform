/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.processor.ApplicabilityNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.RoutingNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.SettingNomadChangeProcessor;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import java.util.List;

import static com.terracottatech.dynamic_config.nomad.processor.ResourceUtil.getResourceAsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigChangeApplicatorIT {
  @Mock
  private ConfigController configController;

  private ConfigChangeApplicator configChangeApplicator;

  @Before
  public void before() {
    configChangeApplicator = new ConfigChangeApplicator(
        new ApplicabilityNomadChangeProcessor(
            configController,
            new RoutingNomadChangeProcessor().register(SettingNomadChange.class, new SettingNomadChangeProcessor(configController))
        )
    );
  }

  @Test
  public void increaseOffheap() throws Exception {
    when(configController.getOffheapSize("primary-server-resource")).thenReturn(Measure.zero(MemoryUnit.class));

    String existing = getResourceAsString("/tc-config.xml");
    String change = getResourceAsString("/increase-offheap-command.json");
    SettingNomadChange settingNomadChange = NomadJson.buildObjectMapper().readValue(change, SettingNomadChange.class);

    PotentialApplicationResult result = configChangeApplicator.canApply(existing, settingNomadChange);

    assertTrue(result.isAllowed());
    Element newConfig = XmlUtils.parse(result.getNewConfiguration()).getDocumentElement();

    Element offheapElement = XmlUtils.getElement(newConfig, "/tc-config/plugins/config/offheap-resources/resource");
    assertEquals("GB", offheapElement.getAttribute("unit"));
    assertEquals("2", offheapElement.getTextContent());


    List<Element> serverElements = XmlUtils.getElements(newConfig, "/tc-config/plugins/config/cluster/stripe/node/server-config/tc-config/plugins/config/offheap-resources/resource");
    assertEquals(4, serverElements.size());
    serverElements.forEach(e -> {
      assertEquals("GB", e.getAttribute("unit"));
      assertEquals("2", e.getTextContent());
    });
  }
}
