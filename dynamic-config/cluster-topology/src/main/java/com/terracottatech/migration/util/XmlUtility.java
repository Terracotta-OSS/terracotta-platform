package com.terracottatech.migration.util;

/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlUtility {

  public static Optional<String> getAttributeValue(Node node, String attributeName) {
    String retValue = null;
    NamedNodeMap attributeMap = node.getAttributes();
    for (int k = 0; k < attributeMap.getLength(); k++) {
      Node attribute = attributeMap.item(k);
      if (attributeName.equals(attribute.getLocalName())) {
        retValue = attribute.getNodeValue();
        break;
      }
    }
    return retValue == null ? Optional.empty() : Optional.of(retValue);
  }

  public static Map<String, String> getAttributeValues(Node node, String... attributes) {
    Set<String> attributeSet = new HashSet<>(Arrays.asList(attributes));
    Map<String, String> retMap = new HashMap<>();
    NamedNodeMap attributeMap = node.getAttributes();
    for (int k = 0; k < attributeMap.getLength(); k++) {
      Node attribute = attributeMap.item(k);
      if (attributeSet.contains(attribute.getLocalName())) {
        retMap.put(attribute.getLocalName(), attribute.getNodeValue());
      }
    }
    return retMap;
  }

  public static String getValue(Node node) {
    return node.getTextContent();
  }

  public static Node createSimpleTextNode(Node documentRoot, String nameSpace, String nodeName, String nodeText) {
    Element element = ((Document)documentRoot).createElementNS(nameSpace, nodeName);
    element.setTextContent(nodeText);
    return element;
  }

  public static Node createNode(Node documentRoot, String nameSpace, String nodeName) {
    return ((Document)documentRoot).createElementNS(nameSpace, nodeName);
  }

  public static Node getClonedParentDocFromRootNode(Node rootNode) {
    Node parentNode = rootNode.getParentNode();
    Node clonedNode = parentNode.cloneNode(true);
    return clonedNode;
  }

  public static String getPrettyPrintableXmlString(Node doc) throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    //initialize StreamResult with File object to save to file
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(doc);
    transformer.transform(source, result);
    String xmlString = result.getWriter().toString();
    return xmlString;
  }

}