package com.fitc.dooropener.control.rulebook;

import java.util.Calendar;

/**
 * Created by Jon on 22/02/2016.
 */
public class TimeRule extends Rule{

    @Override
    boolean isObeyed() {


        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_WEEK);

        // is it after 8am or before 8pm

        return false;
    }
}
