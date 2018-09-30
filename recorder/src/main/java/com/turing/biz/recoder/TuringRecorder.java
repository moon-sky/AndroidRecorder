package com.turing.biz.recoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.turing.biz.recoder.util.PcmToWavUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


class TuringRecorder implements IRecorder, RecorderListener {
    private static final String DEFAULT_PCM_PATH = Environment.getExternalStorageDirectory() + File.separator + "turing.pcm";
    private File sampleFile;
    boolean isRecording = false;
    protected RecorderListener mListener;
    private AudioRecord audioRecord;
    protected int bufferSize;
    private static final String TAG = TuringRecorder.class.getSimpleName();
    protected final int frequency = 16000;
    private int mPlayChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private final int recordConfig = AudioFormat.CHANNEL_IN_MONO;
    protected String pcmFilePath = DEFAULT_PCM_PATH;
    private final ExecutorService singleThreadPool;
    private boolean isPlaying;
    private boolean isAudioThreadProcessing;

    public TuringRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(frequency, recordConfig, audioEncoding);
//        bufferSize = 64*1024;
        singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024));
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public String getPcmFilePath() {
        return pcmFilePath;
    }

    public void setPcmFilePath(String pcmFilePath) {
        this.pcmFilePath = pcmFilePath;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public RecorderListener getmListener() {
        return mListener;
    }

    public void setmListener(RecorderListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public synchronized void startRecord() {
        isRecording = true;
        //生成PCM文件
        sampleFile = new File(getPcmFilePath());
        if (sampleFile.exists()) {
            sampleFile.delete();
        }
        try {
            sampleFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "startRecord: ");
        while (isAudioThreadProcessing){
            Log.d(TAG, "startRecord: 线程未终止，请稍等");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (audioRecord == null) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, recordConfig, audioEncoding, bufferSize);
        }
        audioRecord.startRecording();
        singleThreadPool.execute(new AudioRecordThread());
        onRecordStart();
    }

    @Override
    public synchronized void stopReocrd() {
        Log.d(TAG, "stopReocrd: ");
        isRecording = false;
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
        }
        onRecordStop();

    }

    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public void onRecordStart() {
        if (mListener != null) {
            mListener.onRecordStart();
        }

    }

    @Override
    public void onRecording(byte[] data) {
        if (mListener != null) {
            mListener.onRecording(data);
        }
    }

    @Override
    public void onRecordError(String errorMsg) {
        if (mListener != null) {
            mListener.onRecordError(errorMsg);
        }
    }

    @Override
    public void onRecordStop() {
        if (mListener != null) {
            mListener.onRecordStop();
        }
    }

    public void releaseResource() {
        if (audioRecord != null) {
            audioRecord.release();
        }
        if (!singleThreadPool.isShutdown()) {
            singleThreadPool.shutdown();
        }
    }

    private class AudioRecordThread implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "run: 开始录音线程");
            isAudioThreadProcessing=true;
            if (audioRecord == null) {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, recordConfig, audioEncoding, bufferSize);
            }
            byte[] buffer = new byte[bufferSize];
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(sampleFile));
                int readSize;
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING && isRecording) {
                    readSize = audioRecord.read(buffer, 0, buffer.length);
                    if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                        bos.write(buffer);
                        Log.d(TAG, "run: AudioRecordThread:" + readSize);
                        if (isRecording) {
                            onRecording(buffer);
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //在这里release
                Log.d(TAG, "run: 录音线程结束：" + audioRecord + "|isRecording:" + isRecording+"|curRecorderState:"+audioRecord.getRecordingState());
                audioRecord.release();
                audioRecord = null;
                isAudioThreadProcessing=false;
            }
        }
    }

    private class PlayThread implements Runnable {
        @Override
        public void run() {
            if (sampleFile == null) {
                return;
            }
            //读取文件
            int bufferSize = AudioTrack.getMinBufferSize(frequency,
                    mPlayChannelConfig, audioEncoding);
            byte[] audiodata = new byte[bufferSize];

            try {
                InputStream is = new FileInputStream(sampleFile);
                BufferedInputStream bis = new BufferedInputStream(is);
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        frequency, mPlayChannelConfig, audioEncoding,
                        audiodata.length,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();
                while (bis.available() > 0 && isPlaying) {
                    bis.read(audiodata);
                    audioTrack.write(audiodata, 0, audiodata.length);
                }
                audioTrack.stop();
                bis.close();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "播放失败:" + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "播放失败:" + e.getMessage());
            }
        }
    }

    //播放文件
    public void playRecord(String pcmFilePath) {
        setPcmFilePath(pcmFilePath);
        isPlaying = true;
        singleThreadPool.execute(new PlayThread());
    }

    public boolean convertPcmToWav(String pcmFilePath, String waveFilePath) {
        boolean result = true;
        File wavAudio = new File(waveFilePath);
        if (wavAudio.exists()) {
            wavAudio.delete();
        }
        try {
            wavAudio.createNewFile();
            PcmToWavUtil util = new PcmToWavUtil();
            util.pcmToWav(pcmFilePath, waveFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    //删除文件
    private void deleFile() {
        if (sampleFile == null) {
            return;
        }
        sampleFile.delete();
        Log.d(TAG, "文件删除成功");
    }

}
