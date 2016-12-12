package com.fitc.dooropener.control;

/**
 * Created by Jon on 23/10/2015.
 */
public interface ArduinoUsbProtocol {

   interface Door{
      char SYNC = 'd';

      char OUT_CLOSE = 'c';
      char OUT_OPEN = 'o';
      char OUT_STATUS = 's';
      char OUT_REPEAT_LAST = 'r';
      char IN_CLOSING = '0';
      char IN_CLOSE_ASSIST = '1';
      char  IN_CLOSED = '2';
      char IN_OPENING = '4';
      char IN_OPEN_ASSIST = '5';
      char IN_OPENED = '6';
      char IN_STALLED_OPENING = '7';
      char IN_STALLED_CLOSING = '8';
      char IN_REPEAT_LAST = '9';

   }
}
