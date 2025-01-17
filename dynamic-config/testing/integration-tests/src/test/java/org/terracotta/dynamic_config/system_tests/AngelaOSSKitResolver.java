/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.system_tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.KitResolver;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.topology.LicenseType;
import org.terracotta.angela.common.topology.PackageType;
import org.terracotta.angela.common.topology.Version;
import java.net.URL;
import java.nio.file.Path;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.PackageType.SAG_INSTALLER;
import static org.terracotta.angela.common.util.KitUtils.extractTarGz;
import static org.terracotta.angela.common.util.KitUtils.getParentDirFromTarGz;



public class AngelaOSSKitResolver extends KitResolver  {
  @Override
  public Path resolveLocalInstallerPath(Version version, LicenseType licenseType, PackageType packageType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createLocalInstallFromInstaller(Version version, PackageType packageType, License license, Path localInstallerPath, Path rootInstallationPath, TerracottaCommandLineEnvironment env) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path resolveKitInstallationPath(Version version, PackageType packageType, Path localInstallerPath, Path rootInstallationPath) {
    return rootInstallationPath.resolve(getDirFromArchive(packageType, localInstallerPath));
  }

  private String getDirFromArchive(PackageType packageType, Path localInstaller) {
    if (packageType == KIT) {
      return getParentDirFromTarGz(localInstaller);
    }
    throw new IllegalArgumentException("PackageType " + packageType + " is not supported by " + getClass().getSimpleName());
  }

  @Override
  public URL[] resolveKitUrls(Version version, LicenseType licenseType, PackageType packageType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean supports(LicenseType licenseType) {
    return false;
  }
}
