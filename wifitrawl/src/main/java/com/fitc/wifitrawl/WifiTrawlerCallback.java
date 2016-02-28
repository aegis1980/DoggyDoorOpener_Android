package com.fitc.wifitrawl;

import java.util.List;

/**
 * Created by Jon on 27/02/2016.
 */
public interface WifiTrawlerCallback {
    public void onFinish(List<WifiDevice> devcies);
}
