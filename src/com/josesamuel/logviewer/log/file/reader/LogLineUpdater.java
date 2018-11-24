package com.josesamuel.logviewer.log.file.reader;

import com.android.ddmlib.logcat.LogCatMessage;
import com.josesamuel.logviewer.log.LogProcess;
import com.josesamuel.logviewer.view.AndroidLogcatFormatter;

class LogLineUpdater {

    public static String parseLogLine(String logLine, String fileName, LogProcess logProcess) {
        LogCatMessage message = null;
        try {
            message = AndroidLogcatFormatter.tryParseMessage(logLine);
        } catch (Exception ex) {
        }
        if (message == null) {
            String updatedLogLine = mapLogLines(logLine, fileName, logProcess);
            if (updatedLogLine != null) {
                logLine = updatedLogLine;
            }
        } else {
            logProcess.setProcessID(message.getPid());
            String appName = message.getAppName();
            if (appName == null || appName.isEmpty() || appName.equals("?")) {
                appName = message.getTag();
                if (appName == null || appName.isEmpty() || appName.equals("?")) {
                    appName = "TAG";
                }
            }
            logProcess.setProcessName(appName);
        }
        return logLine;
    }

    private static String mapLogLines(String logLine, String fileName, LogProcess logProcess) {
        try {
            StringBuffer logLineBuilder = new StringBuffer();

            int dateEndIndex = logLine.indexOf(' ');
            if (dateEndIndex == -1) {
                return null;
            }

            int dateStartIndex = logLine.indexOf('-');
            if (dateStartIndex == -1) {
                return null;
            }

            //add date
            logLineBuilder.append(logLine.substring(dateStartIndex + 1, dateEndIndex)).append(' ');


            int timeEndIndex = logLine.indexOf(' ', dateEndIndex + 1);
            if (timeEndIndex == -1) {
                return null;
            }
            //addtime
            logLineBuilder.append(logLine.substring(dateEndIndex + 1, timeEndIndex)).append(' ');

            if (logLine.length() < timeEndIndex + 9) {
                return null;
            }

            String level = logLine.substring(timeEndIndex + 1, timeEndIndex + 10).trim();

            int componentEnd = logLine.indexOf(": ", timeEndIndex + 10);
            if (componentEnd == -1) {
                return null;
            }
            String component = logLine.substring(timeEndIndex + 10, componentEnd);

            int tagEnd = logLine.indexOf(": ", componentEnd + 1);
            if (tagEnd == -1) {
                return null;
            }
            String tag = logLine.substring(componentEnd + 2, tagEnd);

            int process = 0;
            int processStartIndex = tag.indexOf('(');
            if (processStartIndex != -1) {
                int processEndIndex = tag.indexOf(')', processStartIndex);
                if (processEndIndex != -1) {
                    try {
                        process = Integer.parseInt(tag.substring(processStartIndex + 1, processEndIndex));
                    } catch (NumberFormatException ex) {
                    }
                    tag = tag.substring(0, processStartIndex);
                }
            }

            String appName = component;
            String message = logLine.substring(tagEnd + 1).trim();
            if (message.startsWith("(")) {
                int appNameEndIndex = message.indexOf(')');
                if (appNameEndIndex != -1) {
                    appName = message.substring(1, appNameEndIndex);
                }
            }

            //add process
            logLineBuilder.append(process).append("-0/");
            //add appname
            logLineBuilder.append(appName).append(' ');

            //add level
            if (level.equals("DEBUG")) {
                logLineBuilder.append('D');
            } else if (level.equals("INFO")) {
                logLineBuilder.append('I');
            } else if (level.equals("WARNING")) {
                logLineBuilder.append('W');
            } else if (level.equals("ERROR")) {
                logLineBuilder.append('E');
            } else if (level.equals("VERBOSE")) {
                logLineBuilder.append('V');
            } else {
                logLineBuilder.append('D');
            }

            logLineBuilder.append('/');

            if (tag == null || tag.isEmpty()) {
                tag = "TAG";
            }
            //add tag
            logLineBuilder.append(tag).append(": ");


            if (fileName != null && !fileName.isEmpty()) {
                logLineBuilder.append('[').append(fileName).append(']');
            }
            logLineBuilder.append(message);

            logProcess.setProcessName(tag).setProcessID(process);
            return logLineBuilder.toString();
        } catch (Exception ex) {
            return null;
        }
    }

}
