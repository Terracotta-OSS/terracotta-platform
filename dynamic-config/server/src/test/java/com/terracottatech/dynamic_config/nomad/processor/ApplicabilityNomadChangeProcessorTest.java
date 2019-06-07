/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.nomad.Applicability;
import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.nomad.ConfigControllerException;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ApplicabilityNomadChangeProcessorTest {
  @Mock
  private NomadChangeProcessor<NomadChange> underlying;

  private NomadChange change;

  @Captor
  private ArgumentCaptor<Element> elementCaptor;

  @Test
  public void canApplyCluster() throws Exception {
    change = SettingNomadChange.set(Applicability.cluster(), "foo", "bar");
    canApply("0", "1", "2", "3", "");
  }

  @Test
  public void applyCluster() throws Exception {
    change = SettingNomadChange.set(Applicability.cluster(), "foo", "bar");
    apply(true);
  }

  @Test
  public void canApplyThisServer() throws Exception {
    change = SettingNomadChange.set(Applicability.node("Stripe2", "testServer0"), "foo", "bar");
    canApply("0", "");
  }

  @Test
  public void applyThisServer() throws Exception {
    change = SettingNomadChange.set(Applicability.node("Stripe2", "testServer0"), "foo", "bar");
    apply(true);
  }

  @Test
  public void canApplyOtherServer() throws Exception {
    change = SettingNomadChange.set(Applicability.node("Stripe2", "testServer1"), "foo", "bar");
    canApply("1");
  }

  @Test
  public void applyOtherServer() throws Exception {
    change = SettingNomadChange.set(Applicability.node("Stripe2", "testServer1"), "foo", "bar");
    apply(false);
  }

  @Test
  public void canApplyThisStripe() throws Exception {
    change = SettingNomadChange.set(Applicability.stripe("Stripe2"), "foo", "bar");
    canApply("0", "1", "");
  }

  @Test
  public void applyThisStripe() throws Exception {
    change = SettingNomadChange.set(Applicability.stripe("Stripe2"), "foo", "bar");
    apply(true);
  }

  @Test
  public void canApplyOtherStripe() throws Exception {
    change = SettingNomadChange.set(Applicability.stripe("Stripe1"), "foo", "bar");
    canApply("2", "3");
  }

  @Test
  public void applyOtherStripe() throws Exception {
    change = SettingNomadChange.set(Applicability.stripe("Stripe1"), "foo", "bar");
    apply(false);
  }

  private void canApply(String... expectedIds) throws Exception {
    Element rootElement = ResourceUtil.getRootElement("/tc-config.xml");

    ConfigController configController = new ConfigController() {
      @Override
      public String getStripeName() {
        return "Stripe2";
      }

      @Override
      public String getNodeName() {
        return "testServer0";
      }

      @Override
      public Measure<MemoryUnit> getOffheapSize(final String name) throws ConfigControllerException {
        return Measure.zero(MemoryUnit.class);
      }

      @Override
      public void setOffheapSize(final String name, Measure<MemoryUnit> newOffheapSize) throws ConfigControllerException {

      }
    };

    ApplicabilityNomadChangeProcessor commandProcessor = new ApplicabilityNomadChangeProcessor(configController, underlying);
    commandProcessor.canApply(rootElement, change);

    verify(underlying, times(expectedIds.length)).canApply(elementCaptor.capture(), eq(change));

    List<Element> elements = elementCaptor.getAllValues();
    Set<String> ids = elements.stream()
        .map(element -> element.getAttribute("id"))
        .collect(Collectors.toSet());

    assertThat(ids, containsInAnyOrder(expectedIds));
  }

  private void apply(boolean match) throws Exception {
    ConfigController configController = new ConfigController() {
      @Override
      public String getStripeName() {
        return "Stripe2";
      }

      @Override
      public String getNodeName() {
        return "testServer0";
      }

      @Override
      public Measure<MemoryUnit> getOffheapSize(final String name) throws ConfigControllerException {
        return Measure.zero(MemoryUnit.class);
      }

      @Override
      public void setOffheapSize(final String name, Measure<MemoryUnit> newOffheapSize) throws ConfigControllerException {

      }
    };
    ApplicabilityNomadChangeProcessor commandProcessor = new ApplicabilityNomadChangeProcessor(configController, underlying);
    commandProcessor.apply(change);

    if (match) {
      verify(underlying).apply(change);
    } else {
      verifyNoMoreInteractions(underlying);
    }
  }
}
