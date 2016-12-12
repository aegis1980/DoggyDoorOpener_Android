package com.fitc.dooropener.lib.gcm;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Jon on 19/10/2015.
 */
public class GcmDataPayload {


    public class Status{
        public static final int GCM_REGISTER = 0;
        public static final int GCM_REGISTER_ERROR = 1;
        public static final int CONTROL_REPORT = 10;
        public static final int COMMAND_TO_CONTROL = 11;
        public static final int IMAGE_READY_TO_VIEW = 20;

    }

    @SerializedName("status_type")
    private int mStatusType;

    @SerializedName("server_id")
    private String mServerId;
    /*
    Could be oringator remote client or the action at rhe control/ door.
     */
    @SerializedName("originator_id")
    private String mOriginator;
    @SerializedName("controlclient_id")
    private String mControlClientEmail;
    @SerializedName("status_data")
    private String mStatusData;
    @SerializedName("created_at")
    private long mCreatedAt;
    @SerializedName("notification")
    private boolean mSendNotification;

    //*********************************************************************************************
    // getters and setters

    public int getStatusType() {
        return mStatusType;
    }

    public void setStatusType(int mStatusType) {
        this.mStatusType = mStatusType;
    }

    public String getServerId() {
        return mServerId;
    }

    public void setServerId(String mServerId) {
        this.mServerId = mServerId;
    }

    public String getOriginator() {
        return mOriginator;
    }

    public void setOriginator(String mOriginator) {
        this.mOriginator = mOriginator;
    }

    public String getControlClientEmail() {
        return mControlClientEmail;
    }

    public void setControlClientEmail(String mControlClientEmail) {
        this.mControlClientEmail = mControlClientEmail;
    }

    public String getStatusData() {
        return mStatusData;
    }

    public void setStatusData(String mStatusData) {
        this.mStatusData = mStatusData;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public void setCreatedAt(long mCreatedAt) {
        this.mCreatedAt = mCreatedAt;
    }

    public boolean issueNotification() {
        return mSendNotification;
    }

    public void setSendNotification(boolean mSendNotification) {
        this.mSendNotification = mSendNotification;
    }

    //*********************************************************************************************
    // public helpers methods

    @Override
    public String toString() {
        return "Type: " + this.getStatusType() + " Data: " + this.getStatusData();
    }

    //*********************************************************************************************
    // public static helpers methods

    public static GcmDataPayload makeFromJson(String json) {
        return (new Gson()).fromJson(json, GcmDataPayload.class);
    }

    public static String convertToJson(GcmDataPayload payload){
        return (new Gson()).toJson(payload);
    }
}
