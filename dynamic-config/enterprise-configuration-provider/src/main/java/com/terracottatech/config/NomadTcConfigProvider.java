/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.PathResolver;
import com.terracottatech.dynamic_config.xml.XmlConfigMapper;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;

import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public class NomadTcConfigProvider implements TcConfigProvider {
  private final IParameterSubstitutor parameterSubstitutor;

  public NomadTcConfigProvider(IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = requireNonNull(parameterSubstitutor);
  }

  @Override
  public TcConfiguration provide() throws Exception {
    // Sadly platform does not support anything else from XML to load so we have no choice but to re-generate on fly this XML data
    NodeContext configuration = NomadBootstrapper.getNomadServerManager().getConfiguration()
        .orElseThrow(() -> new IllegalStateException("Node has not been activated or migrated properly: unable find the latest committed configuration to use at startup. Please delete the repository folder and try again."));
    // This path resolver is used when converting a model to XML.
    // It makes sure to resolve any relative path to absolute ones based on the working directory.
    // This is necessary because if some relative path ends up in the XML exactly like they are in the model,
    // then platform will rebase these paths relatively to the config XML file which is inside a sub-folder in
    // the config repository: repository/config.
    // So this has the effect of putting all defined directories inside such as repository/config/logs, repository/config/user-data, repository/metadata, etc
    // That is why we need to force the resolving within the XML relatively to the user directory.
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);
    XmlConfigMapper xmlConfigMapper = new XmlConfigMapper(userDirResolver);
    String xml = xmlConfigMapper.toXml(configuration);
    // TCConfigurationParser substitutes values for platform parameters, so anything known to platform needn't be substituted before this
    return TCConfigurationParser.parse(xml);
  }
}
