/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.nomad.Applicability;
import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.nomad.FilteredNomadChange;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.NomadException;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.List;

import static com.terracottatech.dynamic_config.nomad.XmlUtils.getElements;

/**
 * Filters Nomad changes of type {@link FilteredNomadChange} based on their applicability
 */
public class ApplicabilityNomadChangeProcessor implements NomadChangeProcessor<NomadChange> {
  private final ConfigController configController;
  private final NomadChangeProcessor<NomadChange> underlying;

  public ApplicabilityNomadChangeProcessor(ConfigController configController, NomadChangeProcessor<NomadChange> underlying) {
    this.configController = configController;
    this.underlying = underlying;
  }

  @Override
  public void canApply(Element root, NomadChange change) throws NomadException {
    List<Element> applicableElements = getClusterElements(root, change);

    if (applicableToThisServer(change)) {
      applicableElements.add(root);
    }

    for (Element applicableElement : applicableElements) {
      underlying.canApply(applicableElement, change);
    }
  }

  @Override
  public void apply(NomadChange change) throws NomadException {
    if (applicableToThisServer(change)) {
      underlying.apply(change);
    }
  }

  private List<Element> getClusterElements(Element root, NomadChange change) {
    if (!(change instanceof FilteredNomadChange)) {
      return Collections.emptyList();
    }
    Applicability applicability = ((FilteredNomadChange) change).getApplicability();
    try {
      switch (applicability.getType()) {
        case CLUSTER:
          return getElements(root, "/tc-config/plugins/config/cluster/stripe/node/server-config/tc-config");
        case STRIPE:
          return getElements(root,
              "/tc-config/plugins/config/cluster/stripe[name[text() = '%s']]/node/server-config/tc-config",
              applicability.getStripeName());
        case NODE:
          return getElements(root,
              "/tc-config/plugins/config/cluster/stripe[name[text() = '%s']]/node[name[text() = '%s']]/server-config/tc-config",
              applicability.getStripeName(),
              applicability.getNodeName());
        default:
          throw new AssertionError("Unknown applicability: " + applicability);
      }
    } catch (XPathExpressionException e) {
      throw new AssertionError("Bad XPath expression with applicability: " + applicability, e);
    }
  }

  private boolean applicableToThisServer(NomadChange change) {
    if (!(change instanceof FilteredNomadChange)) {
      return false;
    }
    Applicability applicability = ((FilteredNomadChange) change).getApplicability();
    switch (applicability.getType()) {
      case CLUSTER:
        return true;
      case STRIPE:
        return configController.getStripeName().equals(applicability.getStripeName());
      case NODE:
        return configController.getStripeName().equals(applicability.getStripeName())
            && configController.getNodeName().equals(applicability.getNodeName());
      default:
        throw new AssertionError("Unknown applicability: " + applicability);
    }
  }
}
