/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.migration.exception;

import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.BOTH_WHITELIST_AND_DEPR_WHITELIST_PRESENT;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MISMATCHED_CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MISMATCHED_DATA_DIRS;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MISMATCHED_DATA_DIR_NUMBERS;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MISMATCHED_OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MISMATCHED_OFFHEAP_RESOURCE_NUMBERS;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MISMATCHED_SECURITY_CONFIGURATION;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.MULTIPLE_PLATFORM_DATA_DIRS;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.NON_UNIQUE_PLATFORM_DATA_DIR_NAME;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.PLATFORM_DATA_DIR_MISSING_IN_SOME_CONFIG_FILES;
import static com.terracottatech.dynamic_config.server.migration.exception.ErrorCode.UNKNOWN_ERROR;

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