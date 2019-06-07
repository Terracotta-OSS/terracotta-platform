/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.nomad.ConfigChangeApplicatorTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ResourceUtil {
  public static String getResourceAsString(String name) {
    InputStream stream = ConfigChangeApplicatorTest.class.getResourceAsStream(name);
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
  }

  public static Element getRootElement(String resourceName) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(ResourceUtil.class.getResourceAsStream(resourceName));
    return document.getDocumentElement();
  }
}
