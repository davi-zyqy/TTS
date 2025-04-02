package com.example.synthesizerparalleltest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.BuildCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

    private Spinner textSpinner;
    private ArrayAdapter<String> textAdapter;
    private String[] defaultTexts;

//    private final String HH_SPEECH_KEY = System.getenv("HH_SPEECH_KEY");
//    private final String MODEL_KEY = System.getenv("MODEL_KEY");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        editText = findViewById(R.id.editTextTest);
        outputMessage = findViewById(R.id.outputMessageTest);
        synthesizeButton = findViewById(R.id.synthesizeButtonTest);

        defaultTexts = getResources().getStringArray(R.array.default_texts);
        textSpinner = findViewById(R.id.textSpinnerTest);
        textAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, defaultTexts);
        textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textSpinner.setAdapter(textAdapter);


        synthesizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                if (!text.isEmpty()) {
                    submitConcurrentTask(text);
                }

            }
        });

        textSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String timeStamp = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date());
                String selectedItem = (String) parent.getItemAtPosition(position);
                String selectedText = timeStamp + "---" + selectedItem;
                editText.setText(selectedText);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        textSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard();
                return false;
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

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            currentFocus.clearFocus();
        }
    }
}