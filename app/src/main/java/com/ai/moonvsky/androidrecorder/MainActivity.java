package com.ai.moonvsky.androidrecorder;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.turing.biz.recoder.AudioRecordManager;
import com.turing.biz.recoder.RecorderListener;

public class MainActivity extends AppCompatActivity {

    private AudioRecordManager manager;
    boolean isRecording;
    private TextView tv_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = AudioRecordManager.getInstance();
        manager.setListener(new InnerRecorderListenr());
        tv_content = findViewById(R.id.textView);

    }

    private void setText(final String content) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        tv_content.setText(content);
                    }
                }
        );
    }


    public void recorder(View view) {

        isRecording=manager.isRecording();
        if(isRecording){
            ((Button)view).setText(R.string.start_recorder);
            manager.stopRecord();
        }else {
            ((Button)view).setText(R.string.stop_record);
            manager.startRecord();
        }

    }

    public void playRecordAudio(View view) {
        manager.playRecord(manager.getPcmFilePath());
    }

    public void convertToWav(View view) {
        manager.convertPcmToWav(manager.getPcmFilePath(), Environment.getExternalStorageDirectory()+"/test.wav");
    }

    class InnerRecorderListenr implements RecorderListener {

        @Override
        public void onRecordStart() {
            setText(getString(R.string.start_recorder));
        }

        @Override
        public void onRecording(byte[] data) {

        }

        @Override
        public void onRecordError(String errorMsg) {
            setText(getString(R.string.recorder_error));
        }

        @Override
        public void onRecordStop() {
            setText(getString(R.string.recorder_end));
        }
    }
}
