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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private Button longTestButton;
    private Button stopTestButton;

    private Spinner textSpinner;
    private ArrayAdapter<String> textAdapter;
    private String[] defaultTexts;

    private ArrayList<String> logs;

    private ScheduledExecutorService executor;
    private final String[] NAVIGATION_PHRASES= {
        "前方路口右转，合成太快了，稍微把句子搞长一点",
        "保持主路直行，合成太快了，稍微把句子搞长一点",
        "正在重新规划路线，合成太快了，稍微把句子搞长一点",
        "前方300米有测速拍照，合成太快了，稍微把句子搞长一点",
        "请沿当前道路继续行驶1.5公里，合成太快了，稍微把句子搞长一点",
        "目的地在你左侧，合成太快了，稍微把句子搞长一点",
        "请在下个环岛走第二出口，合成太快了，稍微把句子搞长一点",
        "前方进入匝道请减速慢行，合成太快了，稍微把句子搞长一点",
        "已为你避开拥堵路线，合成太快了，稍微把句子搞长一点",
        "预计剩余行程时间15分钟，合成太快了，稍微把句子搞长一点"
    };

//    private final String HH_SPEECH_KEY = System.getenv("HH_SPEECH_KEY");
//    private final String MODEL_KEY = System.getenv("MODEL_KEY");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        editText = findViewById(R.id.editTextTest);
        outputMessage = findViewById(R.id.outputMessageTest);
        synthesizeButton = findViewById(R.id.synthesizeButtonTest);
        longTestButton = findViewById(R.id.longTestButton);
        stopTestButton = findViewById(R.id.stopTestButton);

        defaultTexts = getResources().getStringArray(R.array.default_texts);
        textSpinner = findViewById(R.id.textSpinnerTest);
        textAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, defaultTexts);
        textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textSpinner.setAdapter(textAdapter);

        logs = new ArrayList<>(1024);


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

        longTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                longTimeTest();
            }
        });

        stopTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTest();
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
                .cachePath("/data/local/tmp/Cache")
                .cachingMaxNumber(200)
                .cachingMaxSizeOfOneItemInBytes(10000)
                .cachingCapacityInBytes(10000)
                .ttsVoiceName("Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)")
                .ttsSsmlPattern("<speak xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='http://www.w3.org/2001/mstts' xmlns:emo='http://www.w3.org/2009/10/emotionml' version='1.0' xml:lang='zh-CN'><voice name='Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)' tailingsilence='500ms'>%s</voice></speak>")
                .build();

        synPool = new SimplePool(this, ttsConfig, 5, 10,false);

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

//                synthesizer.setOnCompleted(() -> {
//                    synPool.returnSynthesizer(synthesizer);
//                    currentSynthesisingCnt.decrementAndGet();
//                    updateCounterUI();
//                });

                synthesizer.setOnCompletedListener((fileName, resultId, firstByteLatency, lastByteLatency, duration) -> {
                    Log.i("looktest", resultId);
                    logs.add(fileName);
                    logs.add(resultId);
                    logs.add(firstByteLatency);
                    logs.add(lastByteLatency);
                    logs.add(duration);
                    logs.add("\n");

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

    private void longTimeTest() {
        executor = Executors.newSingleThreadScheduledExecutor();
        final long startTime = System.currentTimeMillis();
        final int[] counter = {0};

        executor.scheduleAtFixedRate(() -> {
            try {
                // 1. 获取当前语句
                String phrase = NAVIGATION_PHRASES[counter[0] % NAVIGATION_PHRASES.length];
                counter[0]++;

                // 2. 提交任务
                submitConcurrentTask(phrase);

                // 3. 超时检查（2小时=7200秒）
                if ((System.currentTimeMillis() - startTime) >= 7200_000) {
                    executor.shutdown();
                    Log.i("Test", "测试正常结束");

                    String fileName = "TTS-LOGS-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".txt";
                    String logPath = "storage/emulated/0/Android/data/com.example.synthesizerparalleltest/files/Dumps/" + fileName;
                    File logFile = new File(logPath);
                    try {
                        if (!logFile.exists()) {
                            boolean created = logFile.createNewFile();
                            if (!created) {
                                Log.e("File", "文件创建失败: " + logPath);
                                return;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Utils.writeToFile(logPath, logs);

                }

                // 4. 每分钟日志
                if (counter[0] % 60 == 0) {
                    Log.i("Test", "已执行次数: " + counter[0]);
                }
            } catch (Exception e) {
                Log.e("Test", "任务异常: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    private void stopTest() {
        if (executor != null) {
            executor.shutdownNow();
            Log.i("Test", "手动终止测试");
        }

        String fileName = "TTS-LOGS-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".txt";
//        String logPath = getExternalFilesDir("/Dumps") + fileName;
        String logPath = "storage/emulated/0/Android/data/com.example.synthesizerparalleltest/files/Dumps/" + fileName;
        File logFile = new File(logPath);
        try {
            if (!logFile.exists()) {
                boolean created = logFile.createNewFile(); // 创建文件
                if (!created) {
                    Log.e("File", "文件创建失败: " + logPath);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.writeToFile(logPath, logs);
    }
}