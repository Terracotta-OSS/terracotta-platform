/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.NomadException;
import org.w3c.dom.Element;

public interface NomadChangeProcessor<T extends NomadChange> {
  void canApply(Element existing, T change) throws NomadException;

  void apply(T change) throws NomadException;
}
