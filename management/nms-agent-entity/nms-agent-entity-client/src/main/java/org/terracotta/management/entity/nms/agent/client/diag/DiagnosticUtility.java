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
package org.terracotta.management.entity.nms.agent.client.diag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

/**
 * @author Anthony Dahanne
 */
public class DiagnosticUtility {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticUtility.class);
  protected static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  /**
   *
   * This method was almost copied / pasted from
   * https://github.com/Terracotta-OSS/terracotta-core/blob/master/common/src/main/java/com/tc/util/runtime/ThreadDumpUtil.java
   * to keep formatting the same as in Terracotta core, without adding a dependency on it.
   *
   *
   * @return a thread dump
   */
  public String getThreadDump() {
    final StringBuilder sb = new StringBuilder(100 * 1024);
    sb.append(new Date());
    sb.append('\n');
    sb.append("Full thread dump ");
    sb.append(System.getProperty("java.vm.name"));
    sb.append(" (");
    sb.append(System.getProperty("java.vm.version"));
    sb.append(' ');
    sb.append(System.getProperty("java.vm.info"));
    sb.append("):\n\n");
    try {
      final ThreadInfo[] threadsInfo = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(),
          threadMXBean.isSynchronizerUsageSupported());

      for (final ThreadInfo threadInfo : threadsInfo) {
        threadHeader(sb, threadInfo);

        final StackTraceElement[] stea = threadInfo.getStackTrace();
        final MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();
        for (StackTraceElement element : stea) {
          sb.append("\tat ");
          sb.append(element.toString());
          sb.append('\n');
          for (final MonitorInfo monitorInfo : monitorInfos) {
            final StackTraceElement lockedFrame = monitorInfo.getLockedStackFrame();
            if (lockedFrame != null && lockedFrame.equals(element)) {
              sb.append("\t- locked <0x");
              sb.append(Integer.toHexString(monitorInfo.getIdentityHashCode()));
              sb.append("> (a ");
              sb.append(monitorInfo.getClassName());
              sb.append(")");
              sb.append('\n');
            }
          }
        }
        if (!threadMXBean.isObjectMonitorUsageSupported() && threadMXBean.isSynchronizerUsageSupported()) {
          sb.append(threadLockedSynchronizers(threadInfo));
        }
        sb.append('\n');
      }
    } catch (final Exception e) {
      LOGGER.error("Cannot take thread dumps - " + e.getMessage(), e);
      sb.append(e);
    }
    return sb.toString();
  }

  private static void threadHeader(StringBuilder sb, ThreadInfo threadInfo) {
    final String threadName = threadInfo.getThreadName();
    sb.append("\"");
    sb.append(threadName);
    sb.append("\" ");
    sb.append("Id=");
    sb.append(threadInfo.getThreadId());

    try {
      final Thread.State threadState = threadInfo.getThreadState();
      final String lockName = threadInfo.getLockName();
      final String lockOwnerName = threadInfo.getLockOwnerName();
      final Long lockOwnerId = threadInfo.getLockOwnerId();
      final boolean isSuspended = threadInfo.isSuspended();
      final boolean isInNative = threadInfo.isInNative();

      sb.append(" ");
      sb.append(threadState);
      if (lockName != null) {
        sb.append(" on ");
        sb.append(lockName);
      }
      if (lockOwnerName != null) {
        sb.append(" owned by \"");
        sb.append(lockOwnerName);
        sb.append("\" Id=");
        sb.append(lockOwnerId);
      }
      if (isSuspended) {
        sb.append(" (suspended)");
      }
      if (isInNative) {
        sb.append(" (in native)");
      }
    } catch (final Exception e) {
      sb.append(" ( Got exception : ").append(e.getMessage()).append(" :");
    }

    sb.append('\n');
  }

  private static String threadLockedSynchronizers(ThreadInfo threadInfo) {
    final String NO_SYNCH_INFO = "no locked synchronizers information available\n";
    if (null == threadInfo) { return NO_SYNCH_INFO; }
    try {
      final LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
      if (lockInfos.length > 0) {
        final StringBuffer lockedSynchBuff = new StringBuffer();
        lockedSynchBuff.append("\nLocked Synchronizers: \n");
        for (final LockInfo lockInfo : lockInfos) {
          lockedSynchBuff.append(lockInfo.getClassName()).append(" <").append(lockInfo.getIdentityHashCode())
              .append("> \n");
        }
        return lockedSynchBuff.append("\n").toString();
      } else {
        return "";
      }
    } catch (final Exception e) {
      return NO_SYNCH_INFO;
    }
  }

}
