package com.example.synthesizerparalleltest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.BuildCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.synthesizerparalleltest.config.TTSConfig;
import com.example.synthesizerparalleltest.config.TTSPolicy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TestActivity extends AppCompatActivity {

    private static final int INTERNET_PERMISSION_REQUEST = 0;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;
    private static final String LOGTAG = "SynthesizerTest";

    private AtomicInteger currentSynthesisingCnt = new AtomicInteger(0);

    //    private Synthesizer synthesizer;
    private ExecutorService executorService;
    private SimplePool synPool;

    private EditText editText;
    private TextView outputMessage;
    private Button synthesizeButton;

//    private final String HH_SPEECH_KEY = System.getenv("HH_SPEECH_KEY");
//    private final String MODEL_KEY = System.getenv("MODEL_KEY");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        editText = findViewById(R.id.editTextTest);
        outputMessage = findViewById(R.id.outputMessageTest);
        synthesizeButton = findViewById(R.id.synthesizeButtonTest);


        synthesizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                submitConcurrentTask(text);
//                if (text.isEmpty()) {
//                    String timeStamp = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date());
//                    String selectedText = timeStamp + "-" + textSpinner.getSelectedItem().toString();
//                    submitConcurrentTask(selectedText);
//                } else {
//                    submitConcurrentTask(text);
//                }
//                synthesizeText(text);

            }
        });

        requestPermissions();
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(TestActivity.this, new String[]{Manifest.permission.INTERNET}, INTERNET_PERMISSION_REQUEST);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(TestActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST);
    }

    private void initializeSpeechSynthesizerPool() {

        TTSConfig ttsConfig = new TTSConfig.Builder()
                .ttsPolicy(TTSPolicy.PARALLEL)
                .ttsKey(BuildConfig.HH_SPEECH_KEY)
                .ttsModelKey(BuildConfig.MODEL_KEY)
                .ttsRegion("francecentral")
                .ttsModelPath("/data/local/tmp/SDCardFiles/TTSModel/XiaoxiaoNeural")
                .ttsVoiceName("Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)")
                .ttsSsmlPattern("<speak xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='http://www.w3.org/2001/mstts' xmlns:emo='http://www.w3.org/2009/10/emotionml' version='1.0' xml:lang='zh-CN'><voice name='Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)' tailingsilence='500ms'>%s</voice></speak>")
                .build();

        synPool = new SimplePool(this, ttsConfig, 5, false);

        // 初始化线程池
        executorService = Executors.newFixedThreadPool(5);
    }

    private void submitConcurrentTask(String text) {
        if (synPool == null) {
            initializeSpeechSynthesizerPool();
        }
        if (!synPool.isSynthesizerAvailable()) {
            return;
        }
        executorService.execute(() -> {
            SimpleSynthesizer synthesizer = synPool.borrowSynthesizer();
            if (synthesizer != null) {

//                synthesizer.setOnCanceled(() -> {
//                    try {
//                        synthesizer.startSynthesizing(text);
//                    } catch (ExecutionException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                });

                synthesizer.setOnCompleted(() -> {
                    synPool.returnSynthesizer(synthesizer);
                    currentSynthesisingCnt.decrementAndGet();
                    updateCounterUI();
                });
                try {
                    synthesizer.startSynthesizing(text);
                    currentSynthesisingCnt.incrementAndGet();
                    updateCounterUI();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                Log.e(LOGTAG, "No available Synthesizer instance");
            }
        });
    }

    private void updateCounterUI() {
        runOnUiThread(() -> {
            outputMessage.setText("当前任务数: " + currentSynthesisingCnt);
        });
    }
}