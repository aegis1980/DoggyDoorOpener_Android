package com.fitc.dooropener.lib.ui;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.SeekBar;

public class SeekBarAnimation extends Animation {
    private SeekBar progressBar;
    private float from;
    private float  to;

    public SeekBarAnimation(SeekBar progressBar, float from, float to) {
        super();
        this.progressBar = progressBar;
        this.from = from;
        this.to = to;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = from + (to - from) * interpolatedTime;
        progressBar.setProgress((int) value);
    }

}