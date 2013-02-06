package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.android.ddmlib.Log.LogLevel;

// Parts of this implementation are from AOSP's LogCatReceiver and LogCatParser.
final class SpoonDeviceLogger {
  private static final String LOGCAT = "logcat -v long";
  private static final int DEVICE_POLL_INTERVAL_MSEC = 1000;
  private static final String TEST_RUNNER = "TestRunner";
  private static final Pattern MESSAGE_START = Pattern.compile("started: ([^(]+)\\(([^)]+)\\)");
  private static final Pattern MESSAGE_END = Pattern.compile("finished: [^(]+\\([^)]+\\)");

  /**
   * This pattern is meant to parse the first line of a log message with the option
   * 'logcat -v long'. The first line represents the date, tag, severity, etc.. while the
   * following lines are the message (can be several lines).<br>
   * This first line looks something like:<br>
   * {@code "[ 00-00 00:00:00.000 <pid>:0x<???> <severity>/<tag>]"}
   * <br>
   * Note: severity is one of V, D, I, W, E, A? or F. However, there doesn't seem to be
   * a way to actually generate an A (assert) message. Log.wtf is supposed to generate
   * a message with severity A, however it generates the undocumented F level. In
   * such a case, the parser will change the level from F to A.<br>
   * Note: the fraction of second value can have any number of digit.<br>
   * Note: the tag should be trimmed as it may have spaces at the end.
   */
  private static final Pattern LOG_HEADER_PATTERN = Pattern.compile(
      "^\\[\\s(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)"
          + "\\s+(\\d*):\\s*(\\S+)\\s([VDIWEAF])/(.*)]$");

  private final List<DeviceLogMessage> messages = new ArrayList<DeviceLogMessage>();
  private final LogCatOutputReceiver receiver = new LogCatOutputReceiver();
  private final IDevice device;
  private LogLevel currentLevel = LogLevel.WARN;
  private String currentPid = "?";
  private String currentTid = "?";
  private String currentTag = "?";
  private String currentTime = "?:??";

  public SpoonDeviceLogger(IDevice device) {
    this.device = device;
    startReceiverThread();
  }

  private void startReceiverThread() {
    new Thread(new Runnable() {
      @Override public void run() {
        // Wait while the device comes online.
        while (device != null && !device.isOnline()) {
          try {
            Thread.sleep(DEVICE_POLL_INTERVAL_MSEC);
          } catch (InterruptedException e) {
            return;
          }
        }

        try {
          if (device != null) {
            device.executeShellCommand(LOGCAT, receiver, 0);
          }
        } catch (Exception e) {
          System.err.println("Unable to connect to logcat on device. Check connection.");
          e.printStackTrace(System.err);
        }
      }
    }).start();
  }

  public Map<DeviceTest, List<DeviceLogMessage>> getParsedLogs() {
      receiver.isCancelled = true;
    Map<DeviceTest, List<DeviceLogMessage>> logs =
        new HashMap<DeviceTest, List<DeviceLogMessage>>();
    DeviceTest current = null;
    String pid = null;
    synchronized (messages) {
      for (DeviceLogMessage message : messages) {
        if (current == null) {
          Matcher match = MESSAGE_START.matcher(message.getMessage());
          if (match.matches() && TEST_RUNNER.equals(message.getTag())) {
            current = new DeviceTest(match.group(2), match.group(1));
            pid = message.getPid();

            List<DeviceLogMessage> deviceLogMessages = new ArrayList<DeviceLogMessage>();
            deviceLogMessages.add(message);
            logs.put(current, deviceLogMessages);
          }
        } else {
          // Only log messages from the same PID.
          if (pid.equals(message.getPid())) {
            logs.get(current).add(message);
          }

          Matcher match = MESSAGE_END.matcher(message.getMessage());
          if (match.matches() && TEST_RUNNER.equals(message.getTag())) {
            current = null;
            pid = null;
          }
        }
      }
    }
    return logs;
  }

  private class LogCatOutputReceiver extends MultiLineReceiver {
    private boolean isCancelled;

    public LogCatOutputReceiver() {
      setTrimLine(false);
    }

    @Override public boolean isCancelled() {
      return isCancelled;
    }

    @Override public void processNewLines(String[] lines) {
      synchronized (messages) {
        processLogLines(lines);
      }
    }
  }

  /**
   * Parse a list of strings into {@link DeviceLogMessage} objects. This method
   * maintains state from previous calls regarding the last seen header of
   * logcat messages.
   *
   * @param lines list of raw strings obtained from logcat -v long
   */
  public void processLogLines(String[] lines) {
    for (String line : lines) {
      if (line.length() == 0) {
        continue;
      }

      Matcher matcher = LOG_HEADER_PATTERN.matcher(line);
      if (matcher.matches()) {
        currentTime = matcher.group(1);
        currentPid = matcher.group(2);
        currentTid = matcher.group(3);
        currentLevel = LogLevel.getByLetterString(matcher.group(4));
        currentTag = matcher.group(5).trim();

        // LogLevel doesn't support messages with severity "F". Log.wtf() is supposed
        // to generate "A", but generates "F".
        if (currentLevel == null && matcher.group(4).equals("F")) {
          currentLevel = LogLevel.ASSERT;
        }
      } else {
        messages.add(
            new DeviceLogMessage(currentLevel, currentPid, currentTid, currentTag, currentTime,
                line));
      }
    }
  }
}