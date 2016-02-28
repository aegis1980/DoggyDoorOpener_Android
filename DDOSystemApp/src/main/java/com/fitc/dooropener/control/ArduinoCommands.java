package com.fitc.dooropener.control;

/**
 * Created by Jon on 23/10/2015.
 */
public interface ArduinoCommands {
    public final static char OUT_CLOSE = 'c';
    public final static char OUT_OPEN = 'o';
    public final static char OUT_STATUS = 's';
    public final static char OUT_REPEAT_LAST = 'r';

    public final static String IN_CLOSING = "0";
    public final static String IN_CLOSE_ASSIST = "1";
    public final static String IN_CLOSED = "2";

    public final static String IN_OPENING = "4";
    public final static String IN_OPEN_ASSIST = "5";
    public final static String IN_OPENED = "6";

    public final static String IN_STALLED_OPENING = "7";
    public final static String IN_STALLED_CLOSING = "8";

    public final static String IN_REPEAT_LAST = "9";
}
