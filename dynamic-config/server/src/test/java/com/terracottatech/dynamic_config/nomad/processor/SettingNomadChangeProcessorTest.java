/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.nomad.Applicability;
import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.nomad.XmlUtils;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SettingNomadChangeProcessorTest {
  @Mock
  private ConfigController configController;

  @Test
  public void canApplySuccess() throws Exception {
    when(configController.getOffheapSize("primary-server-resource")).thenReturn(Measure.of(67108864L, MemoryUnit.B));
    Element rootElement = ResourceUtil.getRootElement("/tc-config.xml");

    SettingNomadChange command = SettingNomadChange.set(Applicability.cluster(), "offheap-resources.primary-server-resource", "2GB");
    SettingNomadChangeProcessor commandProcessor = new SettingNomadChangeProcessor(configController);
    commandProcessor.canApply(rootElement, command);

    XPath xPath = XPathFactory.newInstance().newXPath();
    Element offheapElement = (Element) xPath.evaluate("/tc-config/plugins/config/offheap-resources/resource[@name='primary-server-resource']", rootElement, XPathConstants.NODE);
    assertEquals("GB", offheapElement.getAttribute("unit"));
    assertEquals("2", offheapElement.getTextContent());
  }

  @Test
  public void canApplySuccessCluster() throws Exception {
    when(configController.getOffheapSize("primary-server-resource")).thenReturn(Measure.of(67108864L, MemoryUnit.B));
    Element rootElement = ResourceUtil.getRootElement("/tc-config.xml");
    Element clusterElement = XmlUtils.getElement(rootElement, "/tc-config/plugins/config/cluster/stripe/node/server-config/tc-config[@id='0']");

    SettingNomadChange command = SettingNomadChange.set(Applicability.cluster(), "offheap-resources.primary-server-resource", "2GB");
    SettingNomadChangeProcessor commandProcessor = new SettingNomadChangeProcessor(configController);
    commandProcessor.canApply(clusterElement, command);

    XPath xPath = XPathFactory.newInstance().newXPath();
    Element offheapElement = (Element) xPath.evaluate("//tc-config[@id='0']/plugins/config/offheap-resources/resource[@name='primary-server-resource']", rootElement, XPathConstants.NODE);
    assertEquals("GB", offheapElement.getAttribute("unit"));
    assertEquals("2", offheapElement.getTextContent());
  }

  @Test
  public void applySuccess() throws Exception {
    SettingNomadChange command = SettingNomadChange.set(Applicability.cluster(), "offheap-resources.primary-server-resource", "2GB");
    SettingNomadChangeProcessor commandProcessor = new SettingNomadChangeProcessor(configController);
    commandProcessor.apply(command);

    verify(configController).setOffheapSize("primary-server-resource", Measure.of(2, MemoryUnit.GB));
  }

  @Test(expected = NomadException.class)
  public void canApplyTooSmall() throws Exception {
    when(configController.getOffheapSize("primary-server-resource")).thenReturn(Measure.of(67108864L, MemoryUnit.B));
    Element rootElement = ResourceUtil.getRootElement("/tc-config.xml");

    SettingNomadChange command = SettingNomadChange.set(Applicability.cluster(), "offheap-resources.primary-server-resource", "10MB");
    SettingNomadChangeProcessor commandProcessor = new SettingNomadChangeProcessor(configController);
    commandProcessor.canApply(rootElement, command);
  }
}
