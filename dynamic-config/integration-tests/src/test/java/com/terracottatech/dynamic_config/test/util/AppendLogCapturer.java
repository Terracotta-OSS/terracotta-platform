/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.json.Json;
import com.terracottatech.persistence.sanskrit.JsonUtils;
import com.terracottatech.persistence.sanskrit.MutableSanskritObject;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.SanskritImpl;
import com.terracottatech.persistence.sanskrit.SanskritObject;
import com.terracottatech.persistence.sanskrit.SanskritObjectImpl;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AppendLogCapturer {

  public static List<SanskritObject> getChanges(Path pathToAppendLog) throws SanskritException {
    List<SanskritObject> res = new ArrayList<>();
    ObjectMapper objectMapper = Json.copyObjectMapper();
    new SanskritImpl(new FileBasedFilesystemDirectory(pathToAppendLog), objectMapper) {
      @Override
      public void onNewRecord(String timeStamp, String json) throws SanskritException {
        MutableSanskritObject mutableSanskritObject = new SanskritObjectImpl(objectMapper);
        JsonUtils.parse(objectMapper, json, mutableSanskritObject);
        res.add(mutableSanskritObject);
      }
    };
    return res;
  }
}
