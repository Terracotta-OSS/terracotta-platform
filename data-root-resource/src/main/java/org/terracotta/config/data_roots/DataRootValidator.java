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
package org.terracotta.config.data_roots;

import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ValidationException;
import org.terracotta.data.config.DataDirectories;
import org.terracotta.data.config.DataRootMapping;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataRootValidator implements ConfigValidator {

  private final Function<Element, DataDirectories> resourcesTypeSupplier;

  public DataRootValidator(Function<Element, DataDirectories> resourcesTypeSupplier) {
    this.resourcesTypeSupplier = resourcesTypeSupplier;
  }

  public void validate(Element fragment) throws ValidationException {
    DataDirectories dataDirectories = getDataDirectoriesObject(fragment);
    checkForMultiplePlatformUsage(dataDirectories.getDirectory());
  }

  public void validateAgainst(Element oneFragment, Element otherFragment) throws ValidationException {
    DataDirectories oneDataDirs = getDataDirectoriesObject(oneFragment);
    DataDirectories anotherDataDirs = getDataDirectoriesObject(otherFragment);

    if (oneDataDirs.getDirectory().size() != anotherDataDirs.getDirectory().size()) {
      throw new ValidationException("Number of data-root  directories are not matching."
          , ValidationFailureId.MISMATCHED_DATA_DIR_RESOURCE_NUMBERS.getFailureId());
    }

    Map<String, DataRootMapping> oneDataRootDirectoryNames = oneDataDirs.getDirectory()
        .stream()
        .collect(Collectors.toMap(DataRootMapping::getName, Function.identity()));
    Map<String, DataRootMapping> anotherDataDirectoryNames = anotherDataDirs.getDirectory()
        .stream()
        .collect(Collectors.toMap(DataRootMapping::getName, Function.identity()));
    if (!oneDataRootDirectoryNames.keySet().equals(anotherDataDirectoryNames.keySet())) {
      throw new ValidationException("Mismatched data-root names."
          , ValidationFailureId.MISMATCHED_DATA_DIR_NAMES.getFailureId());
    }

    Set<String> onePlatformMappings = oneDataRootDirectoryNames.entrySet()
        .stream()
        .filter(entry -> entry.getValue().isUseForPlatform())
        .map(drm -> drm.getValue().getName())
        .collect(Collectors.toSet());

    Set<String> anotherPlatformMappings = anotherDataDirectoryNames.entrySet()
        .stream()
        .filter(entry -> entry.getValue().isUseForPlatform())
        .map(drm -> drm.getValue().getName())
        .collect(Collectors.toSet());

    String oneDataRootMappingName = null;
    if (onePlatformMappings.iterator().hasNext()) {
      oneDataRootMappingName = onePlatformMappings.iterator().next();
    }

    String anotherDataRootMappingName = null;
    if (anotherPlatformMappings.iterator().hasNext()) {
      anotherDataRootMappingName = anotherPlatformMappings.iterator().next();
    }

    if ((oneDataRootMappingName == null && anotherDataRootMappingName != null)
        || (oneDataRootMappingName != null && anotherDataRootMappingName == null)) {
      throw new ValidationException("Platform data-root is missing in one."
          , ValidationFailureId.PLATFORM_DATA_ROOT_MISSING_IN_ONE.getFailureId());
    }
    if (oneDataRootMappingName != null) {
      if (!oneDataRootMappingName.equals(anotherDataRootMappingName)) {
        throw new ValidationException("Different data-roots are configured for platform usage in different elements."
            , ValidationFailureId.DIFFERENT_PLATFORM_DATA_ROOTS.getFailureId());
      }
    }
  }

  protected void checkForMultiplePlatformUsage(List<DataRootMapping> currentDataDirs) throws ValidationException {
    List<DataRootMapping> potentialMultiplePlatformUseSet = currentDataDirs.stream().filter(DataRootMapping::isUseForPlatform)
        .collect(Collectors.toList());

    if (potentialMultiplePlatformUseSet.size() >= 2) {
      throw new ValidationException("Multiple data root are defined for platform usage"
          , ValidationFailureId.MULTIPLE_PLATFORM_DATA_ROOTS.getFailureId());
    }
  }

  protected DataDirectories getDataDirectoriesObject(Element fragment) {
    return this.resourcesTypeSupplier.apply(fragment);
  }
}