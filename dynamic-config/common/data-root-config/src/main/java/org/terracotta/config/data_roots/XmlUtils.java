/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package org.terracotta.config.data_roots;

import org.terracotta.data.config.DataDirectories;
import org.terracotta.data.config.ObjectFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import static java.nio.charset.StandardCharsets.UTF_8;

public class XmlUtils {
  static Element getDocumentElement(String xmlString) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    InputSource inputSource = new InputSource(new StringReader(xmlString));
    return documentBuilder.parse(inputSource).getDocumentElement();
  }

  static Element convertToElement(org.terracotta.data.config.DataDirectories dataDirectories) {
    DOMResult res = new DOMResult();
    JAXBElement<DataDirectories> jaxbElement = new ObjectFactory().createDataDirectories(dataDirectories);
    JAXB.marshal(jaxbElement, res);
    return ((Document)res.getNode()).getDocumentElement();
  }

  static String convertToString(Element element) throws Exception {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    transformer.transform(new DOMSource(element), new StreamResult(byteArrayOutputStream));
    return new String(byteArrayOutputStream.toByteArray(), UTF_8);
  }
}
