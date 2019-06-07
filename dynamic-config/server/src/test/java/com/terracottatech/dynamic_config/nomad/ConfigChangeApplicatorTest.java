/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.processor.NomadChangeProcessor;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static com.terracottatech.dynamic_config.nomad.ApplicabilityType.CLUSTER;
import static com.terracottatech.dynamic_config.nomad.ApplicabilityType.NODE;
import static com.terracottatech.dynamic_config.nomad.ApplicabilityType.STRIPE;
import static com.terracottatech.dynamic_config.nomad.processor.ResourceUtil.getResourceAsString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ConfigChangeApplicatorTest {
  @Mock
  private NomadChangeProcessor<NomadChange> commandProcessor;

  @Captor
  private ArgumentCaptor<Element> elementCaptor;

  @Captor
  private ArgumentCaptor<NomadChange> commandCaptor;

  private ConfigChangeApplicator changeApplicator;

  @Before
  public void before() {
    changeApplicator = new ConfigChangeApplicator(commandProcessor);
  }

  @Test
  public void badExistingConfig() {
    PotentialApplicationResult result = changeApplicator.canApply("blah", new SimpleNomadChange("[]", "summary"));

    assertFalse(result.isAllowed());
    assertThat(result.getRejectionReason(), containsString("parsing existing config"));
  }

  @Test
  public void emptyChange() throws Exception {
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", new SimpleNomadChange("[]", "summary"));

    assertTrue(result.isAllowed());
    String newConfig = result.getNewConfiguration();
    Document newConfigDocument = XmlUtils.parse(newConfig);
    Element rootElement = newConfigDocument.getDocumentElement();
    assertEquals("a", rootElement.getTagName());
    assertFalse(rootElement.hasChildNodes());
  }

  @Test
  public void unacceptableChange() throws Exception {
    doThrow(new NomadException("reason")).when(commandProcessor).canApply(any(Element.class), any(NomadChange.class));

    String change = getResourceAsString("/cluster-command.json");
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", new SimpleNomadChange(change, "summary"));

    assertFalse(result.isAllowed());
    assertThat(result.getRejectionReason(), containsString("reason"));
  }

  @Test
  public void clusterChange() throws Exception {
    NomadChange change = parse(getResourceAsString("/cluster-command.json"), NomadChange.class);
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", change);

    assertTrue(result.isAllowed());
    verify(commandProcessor).canApply(elementCaptor.capture(), commandCaptor.capture());

    Element element = elementCaptor.getValue();
    assertEquals("a", element.getTagName());
    assertFalse(element.hasChildNodes());

    NomadChange command = commandCaptor.getValue();
    assertThat(command, is(instanceOf(SettingNomadChange.class)));
    assertEquals(CLUSTER, ((SettingNomadChange) command).getApplicability().getType());
    assertNull(((SettingNomadChange) command).getApplicability().getStripeName());
    assertNull(((SettingNomadChange) command).getApplicability().getNodeName());
    assertEquals("offheap-resources.name", ((SettingNomadChange) command).getName());
    assertEquals("2GB", ((SettingNomadChange) command).getValue());
    assertEquals(SettingNomadChange.Cmd.SET, ((SettingNomadChange) command).getCmd());
  }

  @Test
  public void stripeChange() throws Exception {
    NomadChange change = parse(getResourceAsString("/stripe-command.json"), NomadChange.class);
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", change);

    assertTrue(result.isAllowed());
    verify(commandProcessor).canApply(elementCaptor.capture(), commandCaptor.capture());

    Element element = elementCaptor.getValue();
    assertEquals("a", element.getTagName());
    assertFalse(element.hasChildNodes());

    NomadChange command = commandCaptor.getValue();
    assertThat(command, is(instanceOf(SettingNomadChange.class)));
    assertEquals(STRIPE, ((SettingNomadChange) command).getApplicability().getType());
    assertEquals("stripe1", ((SettingNomadChange) command).getApplicability().getStripeName());
    assertNull(((SettingNomadChange) command).getApplicability().getNodeName());
    assertEquals("offheap-resources.name", ((SettingNomadChange) command).getName());
    assertEquals("2GB", ((SettingNomadChange) command).getValue());
    assertEquals(SettingNomadChange.Cmd.SET, ((SettingNomadChange) command).getCmd());
  }

  @Test
  public void serverChange() throws Exception {
    NomadChange change = parse(getResourceAsString("/server-command.json"), NomadChange.class);
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", change);

    assertTrue(result.isAllowed());
    verify(commandProcessor).canApply(elementCaptor.capture(), commandCaptor.capture());

    Element element = elementCaptor.getValue();
    assertEquals("a", element.getTagName());
    assertFalse(element.hasChildNodes());

    NomadChange command = commandCaptor.getValue();
    assertThat(command, is(instanceOf(SettingNomadChange.class)));
    assertEquals(NODE, ((SettingNomadChange) command).getApplicability().getType());
    assertEquals("stripe1", ((SettingNomadChange) command).getApplicability().getStripeName());
    assertEquals("server1", ((SettingNomadChange) command).getApplicability().getNodeName());
    assertEquals("offheap-resources.name", ((SettingNomadChange) command).getName());
    assertEquals("2GB", ((SettingNomadChange) command).getValue());
    assertEquals(SettingNomadChange.Cmd.SET, ((SettingNomadChange) command).getCmd());
  }

  @Test
  public void elementChange() throws Exception {
    doAnswer(invocation -> {
      Element element = invocation.getArgument(0);
      element.setTextContent("update");
      return null;
    }).when(commandProcessor).canApply(any(Element.class), any(NomadChange.class));

    NomadChange change = parse(getResourceAsString("/cluster-command.json"), NomadChange.class);
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", change);

    assertTrue(result.isAllowed());
    String newConfig = result.getNewConfiguration();
    Document newConfigDocument = XmlUtils.parse(newConfig);
    Element rootElement = newConfigDocument.getDocumentElement();
    assertEquals("a", rootElement.getTagName());
    assertEquals("update", rootElement.getTextContent());
  }

  @Test
  public void multipleCommands() throws Exception {
    doAnswer(invocation -> {
      Element element = invocation.getArgument(0);
      element.setTextContent(element.getTextContent() + ".");
      return null;
    }).when(commandProcessor).canApply(any(Element.class), any(NomadChange.class));

    NomadChange change = parse(getResourceAsString("/multiple-command.json"), NomadChange.class);
    PotentialApplicationResult result = changeApplicator.canApply("<a/>", change);

    assertTrue(result.isAllowed());
    String newConfig = result.getNewConfiguration();
    Document newConfigDocument = XmlUtils.parse(newConfig);
    Element rootElement = newConfigDocument.getDocumentElement();
    assertEquals("a", rootElement.getTagName());
    assertEquals("..", rootElement.getTextContent());

    verify(commandProcessor, times(2)).canApply(elementCaptor.capture(), commandCaptor.capture());

    List<NomadChange> commands = commandCaptor.getAllValues();

    assertThat(commands.get(0), is(instanceOf(SettingNomadChange.class)));
    assertEquals(CLUSTER, ((SettingNomadChange) commands.get(0)).getApplicability().getType());
    assertNull(((SettingNomadChange) commands.get(0)).getApplicability().getStripeName());
    assertNull(((SettingNomadChange) commands.get(0)).getApplicability().getNodeName());
    assertEquals("offheap-resources.name1", ((SettingNomadChange) commands.get(0)).getName());
    assertEquals("2GB", ((SettingNomadChange) commands.get(0)).getValue());
    assertEquals(SettingNomadChange.Cmd.SET, ((SettingNomadChange) commands.get(0)).getCmd());

    assertThat(commands.get(1), is(instanceOf(SettingNomadChange.class)));
    assertEquals(NODE, ((SettingNomadChange) commands.get(1)).getApplicability().getType());
    assertEquals("stripe1", ((SettingNomadChange) commands.get(1)).getApplicability().getStripeName());
    assertEquals("server1", ((SettingNomadChange) commands.get(1)).getApplicability().getNodeName());
    assertEquals("offheap-resources.name2", ((SettingNomadChange) commands.get(1)).getName());
    assertEquals("3MB", ((SettingNomadChange) commands.get(1)).getValue());
    assertEquals(SettingNomadChange.Cmd.SET, ((SettingNomadChange) commands.get(1)).getCmd());
  }

  private NomadChange parse(String json, Class<NomadChange> type) {
    try {
      return NomadJson.buildObjectMapper().readValue(json, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
