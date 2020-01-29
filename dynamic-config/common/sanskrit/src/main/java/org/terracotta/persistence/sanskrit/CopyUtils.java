/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CopyUtils {
  static SanskritObjectImpl makeCopy(ObjectMapper objectMapper, SanskritObject object) {
    if (object == null) {
      return null;
    }

    SanskritObjectImpl copy = new SanskritObjectImpl(objectMapper);
    object.accept(copy);
    return copy;
  }
}
