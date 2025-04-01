package com.example.synthesizerparalleltest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.synthesizerparalleltest.config.TTSConfig;
import com.example.synthesizerparalleltest.config.TTSPolicy;
import com.example.synthesizerparalleltest.events.PlayCompleted;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final int INTERNET_PERMISSION_REQUEST = 0;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;
    private static final String LOGTAG = "SynthesizerTest";

//    private Synthesizer synthesizer;
    private ExecutorService executorService;
    private SynthesizerPool synthesizerPool;
    private SynPool synPool;

    private EditText editText;
    private TextView outputMessage;
    private Button synthesizeButton;
    private Button btnClearFiles;

    private Spinner textSpinner;
    private ArrayAdapter<String> textAdapter;
    private String[] defaultTexts;

    private AtomicInteger currentSynthesisingCnt = new AtomicInteger(0);
    private boolean isDump = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        executorService = Executors.newFixedThreadPool(3);

        editText = findViewById(R.id.editText);
        outputMessage = findViewById(R.id.outputMessage);
        synthesizeButton = findViewById(R.id.synthesizeButton);
        btnClearFiles = findViewById(R.id.btnClearFiles);

        defaultTexts = getResources().getStringArray(R.array.default_texts);
        textSpinner = findViewById(R.id.textSpinner);
        textAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, defaultTexts);
        textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textSpinner.setAdapter(textAdapter);

        synthesizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                if (text.isEmpty()) {
                    String timeStamp = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date());
                    String selectedText = timeStamp + "-" + textSpinner.getSelectedItem().toString();
                    submitConcurrentTask(selectedText);
                } else {
                    submitConcurrentTask(text);
                }
//                synthesizeText(text);

            }
        });

        btnClearFiles.setOnClickListener(v -> {
            String filePath = this.getExternalFilesDir("/Dumps").toString();
            clearFilesInPath(filePath);
        });

        requestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, INTERNET_PERMISSION_REQUEST);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST);
    }

    private void initializeSpeechSynthesizerPool() {

        TTSConfig ttsConfig = new TTSConfig.Builder()
                .ttsPolicy(TTSPolicy.ONLINE)
//                .ttsKey("3fbdccd66e1e44fd926c5861a06ded7d")
                .ttsKey(BuildConfig.HH_SPEECH_KEY)
                .ttsModelKey(BuildConfig.MODEL_KEY)
//                .ttsRegion("chinaeast2")
                .ttsRegion("francecentral")
//                .ttsModelPath("/sdcard/tts/XiaoxiaoNeural")
                .ttsModelPath("/data/local/tmp/SDCardFiles/TTSModel/XiaoxiaoNeural")
                .ttsVoiceName("Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)")
                .ttsSsmlPattern("<speak xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='http://www.w3.org/2001/mstts' xmlns:emo='http://www.w3.org/2009/10/emotionml' version='1.0' xml:lang='zh-CN'><voice name='Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)' tailingsilence='500ms'>%s</voice></speak>")
                .build();

//        TTSConfig ttsConfig = new TTSConfig.Builder()
//                .ttsPolicy(TTSPolicy.OFFLINE)
//                .ttsKey("3fbdccd66e1e44fd926c5861a06ded7d")
//                .ttsRegion("chinaeast2")
//                .ttsModelPath("/data/local/tmp/SDCardFiles/TTSModel/XiaoxiaoHMMSPS")
//                .ttsVoiceName("Microsoft Server Speech Text to Speech Voice (zh-CN, Xiaoxiao)")
//                .ttsSsmlPattern("<speak xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='http://www.w3.org/2001/mstts' xmlns:emo='http://www.w3.org/2009/10/emotionml' version='1.0' xml:lang='zh-CN'><voice name='Microsoft Server Speech Text to Speech Voice (zh-CN, Xiaoxiao)' tailingsilence='500ms'>%s</voice></speak>")
//                .build();

//        synthesizer = new Synthesizer(this, ttsConfig, -1);

//        synthesizerPool = new SynthesizerPool(this, ttsConfig, 3);
        synPool = new SynPool(this, ttsConfig, 5, isDump);

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
            Synthesizer synthesizer = synPool.borrowSynthesizer();
            if (synthesizer != null) {
                try {
                    Runnable onStart = () -> {};
                    Runnable onCompleted = () -> {
                        synPool.returnSynthesizer(synthesizer);
                        currentSynthesisingCnt.decrementAndGet();
                        updateCounterUI();
                    };
                    Runnable onCanceled = () -> {};
                    synthesizer.playStream(text, null, onStart, onCompleted, onCanceled);
                    currentSynthesisingCnt.incrementAndGet();
                    updateCounterUI();
                } finally {

                }

            } else {
                Log.e(LOGTAG, "No available Synthesizer instance");
            }
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayCompleted(PlayCompleted event) {

    }

    private void updateCounterUI() {
        runOnUiThread(() -> {
            outputMessage.setText("当前任务数: " + currentSynthesisingCnt);
        });
    }

    private void clearFilesInPath(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            Toast.makeText(this, "路径不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!directory.isDirectory()) {
            Toast.makeText(this, "路径不是文件夹", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(this, "文件夹为空", Toast.LENGTH_SHORT).show();
            return;
        }

        int deletedCount = 0;
        for (File file : files) {
            if (file.delete()) {
                deletedCount++;
            }
        }

        Toast.makeText(this, "已删除 " + deletedCount + " 个文件", Toast.LENGTH_SHORT).show();
    }

}