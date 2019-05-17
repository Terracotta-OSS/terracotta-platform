/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import org.w3c.dom.Element;

import com.terracottatech.config.br.ObjectFactory;
import com.terracottatech.dynamic_config.xml.Utils;

import java.nio.file.Path;

import javax.xml.bind.JAXBElement;

public class BackupRestore {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private final Path nodeBackupDir;

  public BackupRestore(Path nodeBackupDir) {
    this.nodeBackupDir = nodeBackupDir;
  }

  public Element toElement() {
    com.terracottatech.config.br.BackupRestore backupRestore = createBackupRestore();
    JAXBElement<com.terracottatech.config.br.BackupRestore> jaxbElement = FACTORY.createBackupRestore(backupRestore);
    return Utils.createElement(jaxbElement);
  }

  com.terracottatech.config.br.BackupRestore createBackupRestore() {
    com.terracottatech.config.br.BackupRestore backupRestore = FACTORY.createBackupRestore();

    com.terracottatech.config.br.BackupRestore.BackupLocation backupLocation = FACTORY.createBackupRestoreBackupLocation();

    backupLocation.setPath(nodeBackupDir.toString());

    backupRestore.setBackupLocation(backupLocation);

    return backupRestore;
  }
}
