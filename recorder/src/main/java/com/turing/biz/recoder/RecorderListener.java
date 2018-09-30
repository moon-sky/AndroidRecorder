package com.turing.biz.recoder;

public interface RecorderListener {
    void onRecordStart();
    void onRecording(byte[] data);
    void onRecordError(String errorMsg);
    void onRecordStop();

}
