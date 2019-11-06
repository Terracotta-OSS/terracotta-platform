/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.SanskritImpl;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;
import com.terracottatech.utilities.Json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AppendLogCapturer {

  public static List<JsonNode> getChanges(Path pathToAppendLog) throws SanskritException {
    List<JsonNode> res = new ArrayList<>();
    new SanskritImpl(new FileBasedFilesystemDirectory(pathToAppendLog), Json.copyObjectMapper()) {
      @Override
      public void onNewRecord(String timeStamp, String json) {
        res.add(Json.parse(json));
      }
    };
    return res;
  }
}
