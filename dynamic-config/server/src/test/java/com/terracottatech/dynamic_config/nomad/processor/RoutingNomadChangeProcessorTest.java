/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.server.NomadException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RoutingNomadChangeProcessorTest {
  @Mock
  private NomadChangeProcessor<NomadChange> underlying;

  @Mock
  private Element existing;

  @Mock
  private NomadChange command;

  private RoutingNomadChangeProcessor commandProcessor;

  @Before
  public void before() {
    commandProcessor = new RoutingNomadChangeProcessor().register(command.getClass(), underlying);
  }

  @Test
  public void canApplyValid() throws Exception {
    commandProcessor.canApply(existing, command);
    verify(underlying).canApply(existing, command);
  }

  @Test(expected = NomadException.class)
  public void canApplyNotValid() throws Exception {
    commandProcessor.canApply(existing, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void applyValid() throws Exception {
    commandProcessor.apply(command);
    verify(underlying).apply(command);
  }

  @Test(expected = NomadException.class)
  public void applyNotValid() throws Exception {
    commandProcessor.apply(new SimpleNomadChange("change", "summary"));
  }
}
