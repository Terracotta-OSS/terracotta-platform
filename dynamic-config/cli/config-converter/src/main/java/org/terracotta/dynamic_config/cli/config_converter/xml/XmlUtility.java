/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.config_converter.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Optional;

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

  public static Node getClonedParentDocFromRootNode(Node rootNode) {
    Node parentNode = rootNode.getParentNode();
    return parentNode.cloneNode(true);
  }

  public static void setAttribute(Node node, String attributeName, String attributeValue) {
    boolean match = false;
    NamedNodeMap attributeMap = node.getAttributes();
    for (int k = 0; k < attributeMap.getLength(); k++) {
      Node attribute = attributeMap.item(k);
      if (attributeName.equals(attribute.getLocalName())) {
        attribute.setNodeValue(attributeValue);
        match = true;
        break;
      }
    }
    if (!match) {
      ((Element) node).setAttribute(attributeName, attributeValue);
    }
  }

  public static void removeNode(Node node, boolean removeEmptyParent) {
    Node removalNode = node;
    Node parentNode = node.getParentNode();
    while (parentNode != null) {
      parentNode.removeChild(removalNode);
      if (!removeEmptyParent || parentNode.getChildNodes().getLength() > 0) {
        break;
      }
      removalNode = parentNode;
      parentNode = parentNode.getParentNode();
    }
  }

  public static String getPrettyPrintableXmlString(Node doc) throws TransformerException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    //initialize StreamResult with File object to save to file
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(doc);
    transformer.transform(source, result);
    return result.getWriter().toString();
  }
}
