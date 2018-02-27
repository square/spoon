package com.squareup.spoon;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logError;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;

/**
 * For more information on Android's {@code screenrecord} executable see:
 * https://developer.android.com/studio/command-line/adb.html#screenrecord, https://goo.gl/6deC5j.
 */
final class ScreenRecorder implements Closeable {

  static ScreenRecorder open(
      IDevice device, String deviceOutputDirectoryPath,
      ExecutorService executorService, boolean debug) {
    return new ScreenRecorder(
        device,
        deviceOutputDirectoryPath,
        DEFAULT_RECORD_BUFFER_DURATION_SECONDS,
        DEFAULT_RECORD_BITRATE_MBPS,
        executorService,
        debug);
  }

  private static final String COMMAND_SCREEN_RECORD = "screenrecord";
  private static final String PREFIX_ARGUMENT = "--";
  private static final String ARGUMENT_TIME_LIMIT = PREFIX_ARGUMENT + "time-limit";
  private static final String ARGUMENT_VERBOSE = PREFIX_ARGUMENT + "verbose";
  private static final String ARGUMENT_BITRATE = PREFIX_ARGUMENT + "bit-rate";
  private static final long DEFAULT_RECORD_BUFFER_DURATION_SECONDS = 180L;
  private static final int DEFAULT_RECORD_BITRATE_MBPS = 3000000;

  private final Future<?> mRecordingTask;
  private final AtomicBoolean mDone = new AtomicBoolean();

  private ScreenRecorder(
      IDevice device,
      String deviceOutputDirectoryPath,
      long recordBufferDurationSeconds,
      int recordBitRateMbps,
      ExecutorService executorService,
      boolean debug) {
    mRecordingTask = executorService.submit(() -> {
      int recordingIndex = 0;
      while (!mDone.get()) {
        try {
          String command = Joiner.on(' ').join(new Object[]{
              COMMAND_SCREEN_RECORD,
              ARGUMENT_TIME_LIMIT, recordBufferDurationSeconds,
              ARGUMENT_BITRATE, recordBitRateMbps,
              ARGUMENT_VERBOSE,
              deviceOutputDirectoryPath + '/'
                  + COMMAND_SCREEN_RECORD + '_' + recordingIndex + ".mp4"
          });
          logDebug(debug, "Executing command: [%s]", command);
          StringBuilder outputBuffer = new StringBuilder();
          CountDownLatch completionLatch = new CountDownLatch(1);
          device.executeShellCommand(command, new IShellOutputReceiver() {
            @Override
            public void addOutput(byte[] data, int offset, int length) {
              if (!isCancelled()) {
                outputBuffer.append(new String(data, offset, length, Charsets.UTF_8));
              }
            }

            @Override
            public void flush() {
              completionLatch.countDown();
            }

            @Override
            public boolean isCancelled() {
              return mDone.get();
            }
          });
          if (!mDone.get()) {
            completionLatch.await(recordBufferDurationSeconds * 2, TimeUnit.SECONDS);
          }
          logDebug(debug, "Finished command execution with result: [%S]", outputBuffer.toString());
        } catch (Throwable e) {
          logError("Failed to record: %s", e);
        }
        recordingIndex++;
      }
    });
  }

  @Override
  public void close() throws IOException {
    if (mDone.compareAndSet(false, true)) {
      try {
        mRecordingTask.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new IOException(e);
      }
    }
  }
}