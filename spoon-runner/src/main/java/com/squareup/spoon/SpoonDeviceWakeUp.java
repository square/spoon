package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logError;

/**
 * rhodey (Open Whisper Systems, 2015)
 */
public class SpoonDeviceWakeUp extends Thread implements IShellOutputReceiver {

  private static final long WAKE_UP_DELAY_MS       = 2500L;
  private static final int  POWER_BUTTON_KEY_EVENT = 26;

  private static final String DEVICE_AWAKE_QUERY          = "dumpsys input_method";
  private static final String DEVICE_AWAKE_QUERY_LOLLIPOP = "dumpsys power";
  private static final String DEVICE_AWAKE_REGEX          = ".*mScreenOn=(true|false).*";
  private static final String DEVICE_AWAKE_REGEX_LOLLIPOP = ".*Display Power: state=(ON|OFF).*";

  private enum State {
    CHECK_DEVICE_AWAKE,
    WAKE_UP_DEVICE,
    VERIFY_DEVICE_AWAKE
  }

  private final IDevice       device;
  private final DeviceDetails deviceDetails;
  private final String        serial;
  private final int           adbTimeout;
  private final boolean       debug;

  private State   state       = State.CHECK_DEVICE_AWAKE;
  private boolean deviceAwake = false;
  private String  shellOutput = "";

  public SpoonDeviceWakeUp(IDevice device, DeviceDetails deviceDetails, String serial, int adbTimeout, boolean debug) {
    this.device        = device;
    this.deviceDetails = deviceDetails;
    this.serial        = serial;
    this.adbTimeout    = adbTimeout;
    this.debug         = debug;
  }

  public boolean isDeviceAwake() {
    return deviceAwake;
  }

  private synchronized void handleCheckDeviceAwake() {
    try {

      shellOutput = "";
      logDebug(debug, "[%s] asking device if they are awake", serial);

      if (deviceDetails.getApiLevel() < 21)
        device.executeShellCommand(DEVICE_AWAKE_QUERY, this, adbTimeout, TimeUnit.MILLISECONDS);
      else
        device.executeShellCommand(DEVICE_AWAKE_QUERY_LOLLIPOP, this, adbTimeout, TimeUnit.MILLISECONDS);

    } catch (Exception e) {
      logError("[%s] device didn't like our shell command: %s", serial, e.toString());
      notify();
    }
  }

  private synchronized void handleWakeUpDevice() {
    if (deviceAwake) {
      notify();
      return;
    }

    try {

      logDebug(debug, "[%s] attempting to wake up device", serial);
      device.executeShellCommand("input keyevent " + POWER_BUTTON_KEY_EVENT, this, adbTimeout, TimeUnit.MILLISECONDS);

    } catch (Exception e) {
      logError("[%s] device didn't like our shell command: %s", serial, e.toString());
      notify();
    }
  }

  @Override
  public void run() {
    handleCheckDeviceAwake();
  }

  @Override
  public void addOutput(byte[] data, int offset, int length) {
    if (length < 1 || data.length < (offset + length)) {
      throw new AssertionError("length or offset doesn't make sense");
    }

    byte[] newData = new byte[length];
    for (int i = 0; i < length; i++) {
      newData[i] = data[offset + i];
    }

    shellOutput += new String(newData);
  }

  @Override
  public void flush() {
    Matcher deviceAwakeMatcher;
    String  deviceAwakeCondition;

    if (deviceDetails.getApiLevel() < 21) {
      deviceAwakeMatcher   = Pattern.compile(DEVICE_AWAKE_REGEX, Pattern.DOTALL).matcher(shellOutput);
      deviceAwakeCondition = "true";
    } else {
      deviceAwakeMatcher   = Pattern.compile(DEVICE_AWAKE_REGEX_LOLLIPOP, Pattern.DOTALL).matcher(shellOutput);
      deviceAwakeCondition = "ON";
    }

    synchronized (this) {
      if (!deviceAwakeMatcher.matches() && state != State.WAKE_UP_DEVICE) {
        logError("[%s] unable to determine if device is awake", serial);
        notify();
        return;
      }

      switch (state) {
        case CHECK_DEVICE_AWAKE:
          deviceAwake = deviceAwakeMatcher.group(1).equals(deviceAwakeCondition);
          state       = State.WAKE_UP_DEVICE;
          handleWakeUpDevice();
          break;

        case WAKE_UP_DEVICE:
          state = State.VERIFY_DEVICE_AWAKE;
          try {
            Thread.sleep(WAKE_UP_DELAY_MS);
          } catch (InterruptedException e) { }
          handleCheckDeviceAwake();
          break;

        case VERIFY_DEVICE_AWAKE:
          deviceAwake = deviceAwakeMatcher.group(1).equals(deviceAwakeCondition);
          notify();
          break;
      }
    }
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

}
