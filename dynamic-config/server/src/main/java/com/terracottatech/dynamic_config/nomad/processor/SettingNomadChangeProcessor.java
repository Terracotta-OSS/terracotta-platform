/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.nomad.ConfigControllerException;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.nomad.XmlUtils;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;

import static java.util.Objects.requireNonNull;

/**
 * Supports the processing of {@link SettingNomadChange} for dynamic configuration
 */
public class SettingNomadChangeProcessor implements NomadChangeProcessor<SettingNomadChange> {
  private final ConfigController configController;

  public SettingNomadChangeProcessor(ConfigController configController) {
    this.configController = requireNonNull(configController);
  }

  @Override
  public void canApply(Element existing, SettingNomadChange change) throws NomadException {
    //TODO [DYNAMIC-CONFIG]: TRACK 2: DYNAMIC CONFIG CHANGE:
    // * parse the "name" property as defined in the design doc to determine which setting to change
    // * use common code from config / parameter parser

    // at the moment, for the offheap example, name == "offheap-resources.<resource-name>" and value is "<quantity><unit>"

    String name = change.getName().substring(18);
    Measure<MemoryUnit> newMeasure = Measure.parse(change.getValue(), MemoryUnit.class);

    try {
      Element element = XmlUtils.getElement(existing, "./plugins/config/offheap-resources/resource[@name='%s']", name);

      if (element != null) {
        element.setAttribute("unit", newMeasure.getUnit().name());
        element.setTextContent(Long.toString(newMeasure.getQuantity()));
      }
    } catch (XPathExpressionException e) {
      throw new AssertionError("Bad XPath expression with offheap name: " + name, e);
    }

    Measure<MemoryUnit> existingSize;
    try {
      existingSize = configController.getOffheapSize(name);
    } catch (ConfigControllerException e) {
      throw new NomadException(e);
    }

    if (newMeasure.compareTo(existingSize) < 0) {
      throw new NomadException("New offheap size: " + newMeasure + " is smaller than existing one: " + existingSize);
    }
  }

  @Override
  public void apply(SettingNomadChange change) throws NomadException {
    String name = change.getName().substring(18);
    Measure<MemoryUnit> newMeasure = Measure.parse(change.getValue(), MemoryUnit.class);

    try {
      configController.setOffheapSize(name, newMeasure);
    } catch (ConfigControllerException e) {
      throw new NomadException(e);
    }
  }
}
