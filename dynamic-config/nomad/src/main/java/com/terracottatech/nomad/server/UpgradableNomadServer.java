/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import java.util.List;

public interface UpgradableNomadServer<T> extends NomadServer<T> {
  void setChangeApplicator(ChangeApplicator<T> changeApplicator);
  List<NomadChangeHolder> getAllNomadChanges() throws NomadException;
}
