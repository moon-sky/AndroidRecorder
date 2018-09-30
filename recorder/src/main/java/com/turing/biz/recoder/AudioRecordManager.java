package com.turing.biz.recoder;

import android.text.TextUtils;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class AudioRecordManager {

    private final TuringRecorder recorder;
    private static final String TAG="AudioRecordManager";

    private static class ClassHolder {
        private static AudioRecordManager recordManager = new AudioRecordManager();
    }

    public static AudioRecordManager getInstance() {
        return ClassHolder.recordManager;
    }

    private AudioRecordManager() {
        recorder = new TuringRecorder();
    }

    /**
     * 开始录音
     */
    public synchronized void  startRecord() {
        recorder.startRecord();
    }

    /**
     * 停止录音
     */
    public synchronized void stopRecord() {
        recorder.stopReocrd();
    }

    /**
     * 判断是否在录音
     *
     * @return
     */
    public synchronized boolean isRecording() {
        Log.d(TAG, "isRecording: "+recorder.isRecording);
        return recorder.isRecording;
    }

    public int getBufferSize() {
        if (recorder != null) {
            return recorder.getBufferSize();
        } else {
            return -1;
        }
    }

    public void playRecord(String pcmPath) {
        if (!TextUtils.isEmpty(pcmPath)) {

            recorder.playRecord(pcmPath);
        }
    }

    public boolean isPlaying() {
        return recorder.isPlaying();
    }

    public void stopPlayRecord() {
        recorder.setPlaying(false);
    }

    public String getPcmFilePath() {
        return recorder.getPcmFilePath();
    }

    public void setPcmFilePath(String pcmFilePath) {
        recorder.setPcmFilePath(pcmFilePath);
    }

    /**
     * 转换成pcm
     *
     * @param sourceFile 源文件 pcm格式
     * @param destFile   目标文件 wavg格式
     */
    public void convertPcmToWav(String sourceFile, String destFile) {
        recorder.convertPcmToWav(sourceFile, destFile);
    }

    public void setListener(RecorderListener listener) {
        if (recorder != null) {
            recorder.setmListener(listener);
        }
    }
}
