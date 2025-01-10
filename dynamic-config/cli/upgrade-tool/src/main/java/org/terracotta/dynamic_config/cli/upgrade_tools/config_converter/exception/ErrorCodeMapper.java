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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception;

import java.util.HashMap;
import java.util.Map;

import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.BOTH_WHITELIST_AND_DEPR_WHITELIST_PRESENT;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MISMATCHED_CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MISMATCHED_DATA_DIRS;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MISMATCHED_DATA_DIR_NUMBERS;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MISMATCHED_OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MISMATCHED_OFFHEAP_RESOURCE_NUMBERS;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MISMATCHED_SECURITY_CONFIGURATION;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.MULTIPLE_PLATFORM_DATA_DIRS;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.NON_UNIQUE_PLATFORM_DATA_DIR_NAME;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.PLATFORM_DATA_DIR_MISSING_IN_SOME_CONFIG_FILES;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ErrorCode.UNKNOWN_ERROR;

public class ErrorCodeMapper {

  private static final Map<Long, ErrorDetail> ERROR_MAP = new HashMap<>();

  static {
    //Data Root Validation Error Code Mappings
    ERROR_MAP.put(800001L, new ErrorDetail(MISMATCHED_DATA_DIR_NUMBERS
        , "Number of data directories defined in %s and in %s are not matching"));
    ERROR_MAP.put(800002L, new ErrorDetail(PLATFORM_DATA_DIR_MISSING_IN_SOME_CONFIG_FILES
        , "Platform data root missing in one of %s %s"));
    ERROR_MAP.put(800003L, new ErrorDetail(MISMATCHED_DATA_DIRS
        , "Data root directories are not same in %s and %s"));
    ERROR_MAP.put(800004L, new ErrorDetail(NON_UNIQUE_PLATFORM_DATA_DIR_NAME
        , "Different data root directories are defined in %s %s"));
    ERROR_MAP.put(800005L, new ErrorDetail(MULTIPLE_PLATFORM_DATA_DIRS,
        "Multiple data root directories are defined in %s"));

    //Security Validation Error Code Mappings
    ERROR_MAP.put(900001L, new ErrorDetail(BOTH_WHITELIST_AND_DEPR_WHITELIST_PRESENT
        , "Both elements <whitelist> and <white-list>"));
    ERROR_MAP.put(900002L, new ErrorDetail(MISMATCHED_SECURITY_CONFIGURATION
        , "Mismatched Security configuration in %s"));

    //Lease
    ERROR_MAP.put(700001L, new ErrorDetail(MISMATCHED_CLIENT_LEASE_DURATION
        , "Mismatched lease configuration in %s"));

    //OffHeap
    ERROR_MAP.put(500001L, new ErrorDetail(MISMATCHED_OFFHEAP_RESOURCE_NUMBERS
        , "Different number of off-heap resources are defined in %s and %s"));
    ERROR_MAP.put(500002L, new ErrorDetail(MISMATCHED_OFFHEAP_RESOURCES
        , "Mismatched off-heap configuration in %s and %s"));

  }

  public static ErrorDetail getErrorCode(long projectSpecificErrorId) {
    ErrorDetail errorDetail = ERROR_MAP.get(projectSpecificErrorId);
    if (errorDetail == null) {
      errorDetail = new ErrorDetail(UNKNOWN_ERROR, "Unknown error with id " + projectSpecificErrorId);
    }
    return errorDetail;
  }

  public static class ErrorDetail {
    final ErrorCode errorCode;
    final String errorMessage;

    ErrorDetail(ErrorCode errorCode, String errorMessage) {
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
    }

    public ErrorCode getErrorCode() {
      return errorCode;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}