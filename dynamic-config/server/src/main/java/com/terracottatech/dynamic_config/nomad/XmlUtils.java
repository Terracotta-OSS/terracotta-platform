/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XmlUtils {
  private static TransformerFactory transformerFactory = TransformerFactory.newInstance();

  public static Document parse(String documentAsString) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    InputSource inputSource = new InputSource(new StringReader(documentAsString));
    return documentBuilder.parse(inputSource);
  }

  public static String render(Document document) throws TransformerException {
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    StringWriter stringWriter = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
    return stringWriter.toString();
  }

  public static List<Element> getElements(Element root, String expressionFormat, String... values) throws XPathExpressionException {
    String expression = calculateExpression(expressionFormat, values);

    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList nodes = (NodeList) xPath.evaluate(expression, root, XPathConstants.NODESET);

    List<Element> elements = new ArrayList<>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); ++i) {
      Element element = (Element) nodes.item(i);
      elements.add(element);
    }

    return elements;
  }

  public static Element getElement(Element root, String expressionFormat, String... values) throws XPathExpressionException {
    String expression = calculateExpression(expressionFormat, values);

    XPath xPath = XPathFactory.newInstance().newXPath();
    Node node = (Node) xPath.evaluate(expression, root, XPathConstants.NODE);

    if (!(node instanceof Element)) {
      throw new XPathExpressionException("Expression did not evaluate to an element: " + expression);
    }

    return (Element) node;
  }

  private static String calculateExpression(String expressionFormat, String[] values) {
    Object[] escapedValues = Arrays.stream(values).map(XmlUtils::escape).toArray(Object[]::new);
    return String.format(expressionFormat, escapedValues);
  }

  private static String escape(String value) {
    return value.replace("'", "\\'");
  }
}
