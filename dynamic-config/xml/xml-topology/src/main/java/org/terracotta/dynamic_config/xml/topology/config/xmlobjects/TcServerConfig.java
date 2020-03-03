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
package org.terracotta.dynamic_config.xml.topology.config.xmlobjects;

import org.terracotta.config.TcConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tc-server-config complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tc-server-config"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element ref="{http://www.terracotta.org/config}tc-config"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tc-server-config", propOrder = {

})
public class TcServerConfig {

  @XmlElement(name = "tc-config", namespace = "http://www.terracotta.org/config", required = true)
  protected TcConfig tcConfig;

  /**
   * This section defines the list of plugins which are provided to the server by the platform.
   * Each plugin provider defines their own schema and parsing responsibility is left up to the plugin provider.
   *
   * @return possible object is
   * {@link TcConfig }
   */
  public TcConfig getTcConfig() {
    return tcConfig;
  }

  /**
   * Sets the value of the tcConfig property.
   *
   * @param value allowed object is
   *              {@link TcConfig }
   */
  public void setTcConfig(TcConfig value) {
    this.tcConfig = value;
  }

}