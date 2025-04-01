package com.example.synthesizerparalleltest;

import android.content.Context;
import android.util.Log;

import com.example.synthesizerparalleltest.config.TTSConfig;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimplePool {
    private static final String LOGTAG = "SynthesizerPool";
    private final LinkedList<SimpleSynthesizer> pool = new LinkedList<>();
    //    private ThreadLocal<Synthesizer> currentSynthesizer = new ThreadLocal<>();
    private final Lock poolLock = new ReentrantLock();
    private final int maxPoolSize;
    private final TTSConfig baseConfig;
    private final Context context;
    private boolean isDump;

    public SimplePool(Context context, TTSConfig ttsConfig, int poolSize, boolean isDump) {
//        this.pool = new LinkedBlockingQueue<>(poolSize);
        this.maxPoolSize = poolSize;
        this.isDump = isDump;
        this.baseConfig = ttsConfig;
        this.context = context;
        initializePool();
//        EventBus.getDefault().register(this);
//        for (int i = 0; i < poolSize; i++) {
//            try {
//                pool.put(new Synthesizer(context, ttsConfig, i, isDump));
//            }catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new RuntimeException("初始化对象池失败", e);
//            }
//
//        }
    }
    private void initializePool() {
        for (int i = 0; i < maxPoolSize; i++) {
            SimpleSynthesizer synthesizer = createNewSynthesizer();
            if (synthesizer != null) {
                pool.add(synthesizer);
            }
        }
    }

    private SimpleSynthesizer createNewSynthesizer() {
        try {

            TTSConfig configCopy = new TTSConfig.Builder()
                    .ttsPolicy(baseConfig.ttsPolicy)
                    .ttsKey(baseConfig.ttsKey)
                    .ttsModelKey(baseConfig.ttsModelKey)
                    .ttsRegion(baseConfig.ttsRegion)
                    .ttsModelPath(baseConfig.ttsModelPath)
                    .ttsVoiceName(baseConfig.ttsVoiceName)
                    .ttsSsmlPattern(baseConfig.ttsSsmlPattern)
                    .build();

            return new SimpleSynthesizer(context, configCopy);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to create Synthesizer: " + e.getMessage());
            return null;
        }
    }

    public SimpleSynthesizer borrowSynthesizer() {
        poolLock.lock();
        try {
            if (!pool.isEmpty()) {
//                return pool.removeFirst();
                SimpleSynthesizer synthesizer = pool.removeFirst();
                // 设置回调：归还当前实例到池中
//                synthesizer.setOnCompleted(() -> returnSynthesizer(synthesizer));
                return synthesizer;
            }

            // return createNewSynthesizer();
            return null;
        } finally {
            poolLock.unlock();
        }
    }

    public boolean isSynthesizerAvailable() {
        return !pool.isEmpty();
    }


    public void returnSynthesizer(SimpleSynthesizer synthesizer) {
        if (synthesizer == null) {
            return;
        }
        poolLock.lock();
        try {
            if (pool.size() < maxPoolSize) {
                pool.addLast(synthesizer);
            }
        } finally {
            poolLock.unlock();
        }

    }
}
