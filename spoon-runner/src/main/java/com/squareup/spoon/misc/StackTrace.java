package com.squareup.spoon.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/** A representation of {@link Throwable} suitable for serialization. */
public class StackTrace {
  private static final Pattern HEADER = Pattern.compile("(?:Caused by: )?([^:]+)(?:: (.*))?");
  private static final Pattern MORE = Pattern.compile("\\.\\.\\. \\d+ more");
  private static final Pattern ELEMENT =
      Pattern.compile("\\s*at (.*?)\\.([^.(]+)\\((?:([^:]+):(\\d+)|Native Method)\\)");

  /** Convert a {@link Throwable} to its equivalent {@link StackTrace}. */
  public static StackTrace from(Throwable exception) {
    checkNotNull(exception);

    StackTrace cause = null;
    Throwable realCause = exception.getCause();
    if (realCause != null && realCause != exception) {
      cause = from(realCause);
    }

    List<Element> elements = new ArrayList<Element>();
    for (StackTraceElement element : exception.getStackTrace()) {
      elements.add(Element.from(element));
    }

    String className = exception.getClass().getCanonicalName();
    String message = exception.getMessage();
    return new StackTrace(className, message, elements, cause);
  }

  public static StackTrace from(String exception) {
    checkNotNull(exception);
    String parts[] = exception.replace("\r\n", "\n").split("\n");

    StackTrace last = null;
    List<String> messageParts = new ArrayList<String>();
    List<Element> elements = new ArrayList<Element>();
    boolean matchingElements = true; // Assume we will be matching elements first (bottom, up).
    for (int i = parts.length - 1; i >= 0; i--) {
      String part = parts[i];

      Matcher elementMatch = ELEMENT.matcher(part);
      Matcher moreMatch = MORE.matcher(part);
      boolean moreMatches = moreMatch.matches();
      if (elementMatch.matches() || moreMatches) {
        if (!matchingElements) {
          last = acceptTrace(messageParts, elements, last);
          elements.clear();
          messageParts.clear();
        }
        matchingElements = true;

        if (!moreMatches) {
          String className = elementMatch.group(1);
          String methodName = elementMatch.group(2);
          String fileName = elementMatch.group(3);
          boolean isNative = fileName == null;
          int line = isNative ? 0 : Integer.parseInt(elementMatch.group(4));

          elements.add(0, new Element(className, fileName, line, methodName, isNative));
        }
      } else {
        matchingElements = false;
        messageParts.add(0, part);
      }
    }

    return acceptTrace(messageParts, elements, last);
  }

  private static StackTrace acceptTrace(List<String> messageParts, List<Element> elements,
      StackTrace last) {
    String header = messageParts.remove(0);
    Matcher headerMatch = HEADER.matcher(header);
    if (!headerMatch.matches()) {
      throw new IllegalStateException("Couldn't match exception header.");
    }
    messageParts.add(0, headerMatch.group(2));
    String exceptionClass = headerMatch.group(1);
    String message = StringUtils.join(messageParts, "\n");
    if (message.equals("")) {
      message = null;
    }
    return new StackTrace(exceptionClass, message, elements, last);
  }

  private final String className;
  private final String message;
  private final List<Element> elements;
  private final StackTrace cause;

  public StackTrace(String className, String message, List<Element> elements, StackTrace cause) {
    checkNotNull(elements);

    this.className = className;
    this.message = message;
    this.elements = Collections.unmodifiableList(new ArrayList<Element>(elements));
    this.cause = cause;
  }

  public String getClassName() {
    return className;
  }

  public String getMessage() {
    return message;
  }

  public List<Element> getElements() {
    return elements;
  }

  public StackTrace getCause() {
    return cause;
  }

  @Override public String toString() {
    if (className != null) {
      if (message != null) {
        return className + ": " + message;
      }
      return className;
    }
    return message;
  }

  /** A representation of {@link StackTraceElement} suitable for serialization. */
  public static class Element {
    static Element from(StackTraceElement e) {
      return new Element(e.getClassName(), e.getFileName(), e.getLineNumber(), e.getMethodName(),
          e.isNativeMethod());
    }

    private final String className;
    private final String fileName;
    private final int line;
    private final String methodName;
    private final boolean isNative;

    public Element(String className, String fileName, int line, String methodName,
        boolean isNative) {
      this.className = className;
      this.fileName = fileName;
      this.line = line;
      this.methodName = methodName;
      this.isNative = isNative;
    }

    public String getClassName() {
      return className;
    }

    public String getFileName() {
      return fileName;
    }

    public int getLine() {
      return line;
    }

    public String getMethodName() {
      return methodName;
    }

    public boolean isNative() {
      return isNative;
    }

    @Override public String toString() {
      if (isNative) {
        return String.format("%s.%s(Native Method)", className, methodName);
      }
      return String.format("%s.%s(%s:%d)", className, methodName, fileName, line);
    }
  }
}
