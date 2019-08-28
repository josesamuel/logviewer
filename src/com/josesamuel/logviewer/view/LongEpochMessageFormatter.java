package com.josesamuel.logviewer.view;


import com.android.ddmlib.Log;
import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatLongEpochMessageParser;
import com.android.ddmlib.logcat.LogCatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LongEpochMessageFormatter implements MessageFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER;
    private static final Pattern DATE_TIME;
    private static final Pattern EPOCH_TIME_HEADER_MESSAGE;
    private static final Pattern DATE_TIME_HEADER_MESSAGE;
    private final AndroidLogcatPreferences myPreferences;
    private final ZoneId myTimeZone;

    LongEpochMessageFormatter(@NotNull AndroidLogcatPreferences preferences, @NotNull ZoneId timeZone) {
        this.myPreferences = preferences;
        this.myTimeZone = timeZone;
    }

    @NotNull
    public String format(@NotNull String format, @NotNull LogCatHeader header, @NotNull String message) {
        Instant timestampInstant = header.getTimestampInstant();

        assert timestampInstant != null;

        //Object timestampString = this.myPreferences.SHOW_AS_SECONDS_SINCE_EPOCH ? LogCatLongEpochMessageParser.EPOCH_TIME_FORMATTER.format(timestampInstant) : DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(timestampInstant, this.myTimeZone));
        Object timestampString = DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(timestampInstant, this.myTimeZone));
        Object processIdThreadId = header.getPid() + "-" + header.getTid();
        Object priority = header.getLogLevel().getPriorityLetter();
        Object tag = header.getTag().replace(' ', ' ');
        return String.format(Locale.ROOT, format, timestampString, processIdThreadId, header.getAppName(), priority, tag, message);
    }

    @Nullable
    public LogCatMessage tryParse(@NotNull String message) {
        //Matcher matcher = this.myPreferences.SHOW_AS_SECONDS_SINCE_EPOCH ? EPOCH_TIME_HEADER_MESSAGE.matcher(message) : DATE_TIME_HEADER_MESSAGE.matcher(message);
        Matcher matcher = DATE_TIME_HEADER_MESSAGE.matcher(message);
        if (!matcher.matches()) {
            return null;
        } else {
            Log.LogLevel priority = Log.LogLevel.getByLetterString(matcher.group(5));

            assert priority != null;

            int processId = Integer.parseInt(matcher.group(2));
            int threadId = Integer.parseInt(matcher.group(3));
            String tag = matcher.group(6);
            Instant timestampInstant;
//            if (this.myPreferences.SHOW_AS_SECONDS_SINCE_EPOCH) {
//                timestampInstant = (Instant)LogCatLongEpochMessageParser.EPOCH_TIME_FORMATTER.parse(matcher.group(1), Instant::from);
//            } else {
                LocalDateTime timestampDateTime = LocalDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);
                timestampInstant = timestampDateTime.toInstant(this.myTimeZone.getRules().getOffset(timestampDateTime));
//            }

            LogCatHeader header = new LogCatHeader(priority, processId, threadId, matcher.group(4), tag, timestampInstant);
            return new LogCatMessage(header, matcher.group(7));
        }
    }

    static {
        DATE_TIME_FORMATTER = (new DateTimeFormatterBuilder()).append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(' ').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true).toFormatter(Locale.ROOT);
        DATE_TIME = Pattern.compile("\\d+-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d");
        EPOCH_TIME_HEADER_MESSAGE = Pattern.compile("^(" + LogCatLongEpochMessageParser.EPOCH_TIME + ") +(" + PROCESS_ID + ")-(" + THREAD_ID + ")/(" + PACKAGE + ") (" + PRIORITY + ")/(" + TAG + "): (" + MESSAGE + ")$");
        DATE_TIME_HEADER_MESSAGE = Pattern.compile("^(" + DATE_TIME + ") +(" + PROCESS_ID + ")-(" + THREAD_ID + ")/(" + PACKAGE + ") (" + PRIORITY + ")/(" + TAG + "): (" + MESSAGE + ")$");
    }
}
