/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.config.data_roots.management;

import org.terracotta.management.service.monitoring.registry.provider.AliasBinding;

import java.nio.file.Path;

public class DataRootBinding extends AliasBinding {

  public DataRootBinding(String identifier, Path path) {
    super(identifier, path);
  }

  @Override
  public Path getValue() {
    return (Path) super.getValue();
  }

}
