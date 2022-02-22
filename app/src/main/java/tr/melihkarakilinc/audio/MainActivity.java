package tr.melihkarakilinc.audio;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    Button startRec, stopRec, playBack;
    int minBufferSizeIn;
    AudioRecord audioRecord;
    short[] audioData;
    Boolean recording;
    int sampleRateInHz = 48000;
    private String TAG = "TAG";

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startRec = (Button) findViewById(R.id.startrec);
        stopRec = (Button) findViewById(R.id.stoprec);
        playBack = (Button) findViewById(R.id.playback);

        startRec.setOnClickListener(startRecOnClickListener);
        stopRec.setOnClickListener(stopRecOnClickListener);
        playBack.setOnClickListener(playBackOnClickListener);
        playBack.setEnabled(false);
        startRec.setEnabled(true);
        stopRec.setEnabled(false);

        minBufferSizeIn = AudioRecord.getMinBufferSize(sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioData = new short[minBufferSizeIn];

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSizeIn);

    }

    View.OnClickListener startRecOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            playBack.setEnabled(false);
            startRec.setEnabled(false);
            stopRec.setEnabled(true);
            Thread recordThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    recording = true;
                    startRecord();
                }

            });

            recordThread.start();

        }
    };


    View.OnClickListener stopRecOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            playBack.setEnabled(true);
            startRec.setEnabled(false);
            stopRec.setEnabled(false);
            recording = false;
        }
    };



    View.OnClickListener playBackOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            playBack.setEnabled(false);
            startRec.setEnabled(true);
            stopRec.setEnabled(false);
            playRecord();
        }

    };


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void startRecord() {
        File file = new File(Environment.getExternalStorageDirectory(), "test.pcm");

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            NoiseSuppressor ns;
            AcousticEchoCanceler aec;

            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(audioRecord.getAudioSessionId());
                if (ns != null) {
                    ns.setEnabled(true);
                } else {
                    Log.e(TAG, "AudioInput: NoiseSuppressor is null and not enabled");
                }
            }

            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
                if (aec != null) {
                    aec.setEnabled(true);
                } else {
                    Log.e(TAG, "AudioInput: AcousticEchoCanceler is null and not enabled");
                }
            }
            audioRecord.startRecording();

            while (recording) {
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSizeIn);
                for (int i = 0; i < numberOfShort; i++) {
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void playRecord() {
        File file = new File(Environment.getExternalStorageDirectory(), "test.pcm");

        int shortSizeInBytes = Short.SIZE / Byte.SIZE;

        int bufferSizeInBytes = (int) (file.length() / shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while (dataInputStream.available() > 0) {
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, sampleRateInHz,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM,
                    audioRecord.getAudioSessionId());
            while(audioTrack.getState() != AudioTrack.STATE_INITIALIZED){

            }
            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}