/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.util;

public class CommonParamsUtils {
  public static String[] splitQuantityUnit(String quantityUnit) {
    char[] chars = quantityUnit.toCharArray();
    int i;
    for (i = 0; i < chars.length; i++) {
      if (!Character.isDigit(chars[i])) {
        break;
      }
    }

    if (i == quantityUnit.length()) {
      return new String[] {quantityUnit};
    } else {
      return new String[] {quantityUnit.substring(0, i), quantityUnit.substring(i)};
    }
  }

  public static boolean nullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
