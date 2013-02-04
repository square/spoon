/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.spoon;

import com.android.ddmlib.Log.LogLevel;

/**
 * Model a single log message output from {@code logcat -v long}. A logcat message has a {@link
 * LogLevel}, the pid (process id) of the process generating the message, the time at which the
 * message was generated, and the tag and message itself.
 */
public final class DeviceLogMessage {
  private final LogLevel logLevel;
  private final String pid;
  private final String tid;
  private final String tag;
  private final String time;
  private final String message;

  /** Construct an immutable log message object. */
  DeviceLogMessage(LogLevel logLevel, String pid, String tid, String tag, String time,
      String message) {
    this.logLevel = logLevel;
    this.pid = pid;
    this.tag = tag;
    this.time = time;
    this.message = message;

    long tidValue;
    try {
      // Thread id's may be in hex on some platforms. Decode and store them in radix 10.
      tidValue = Long.decode(tid.trim());
    } catch (NumberFormatException e) {
      tidValue = -1;
    }

    this.tid = Long.toString(tidValue);
  }

  public LogLevel getLevel() {
    return logLevel;
  }

  public String getPid() {
    return pid;
  }

  public String getTid() {
    return tid;
  }

  public String getTag() {
    return tag;
  }

  public String getTime() {
    return time;
  }

  public String getMessage() {
    return message;
  }

  @Override public String toString() {
    return time + ": " + logLevel.getPriorityLetter() + "/" + tag + "(" + pid + "): " + message;
  }
}