package com.example.synthesizerparalleltest;

import android.content.Context;
import android.util.Log;

import com.example.synthesizerparalleltest.config.TTSConfig;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynthesizerPool {
    private static final String LOGTAG = "SynthesizerPool";
    private final int maxPoolSize;
    private final LinkedList<Synthesizer> pool = new LinkedList<>();
    private final Lock poolLock = new ReentrantLock();
    private final Context context;
    private final TTSConfig baseConfig;

    public SynthesizerPool(Context context, TTSConfig baseConfig, int maxPoolSize) {
        this.context = context;
        this.baseConfig = baseConfig;
        this.maxPoolSize = maxPoolSize;
        initializePool();
    }

    private void initializePool() {
        for (int i = 0; i < maxPoolSize; i++) {
            Synthesizer synthesizer = createNewSynthesizer();
            if (synthesizer != null) {
                pool.add(synthesizer);
            }
        }
    }

    // 创建新实例
    private Synthesizer createNewSynthesizer() {
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
            return new Synthesizer(context, configCopy, -1, false);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to create Synthesizer: " + e.getMessage());
            return null;
        }
    }


    public Synthesizer borrowSynthesizer() {
        poolLock.lock();
        try {
            if (!pool.isEmpty()) {
                return pool.removeFirst();
            }

            // return createNewSynthesizer();
            return null;
        } finally {
            poolLock.unlock();
        }
    }


    public void returnSynthesizer(Synthesizer synthesizer) {
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

//    public void shutdown() {
//        poolLock.lock();
//        try {
//            for (Synthesizer synth : pool) {
//                synth.release();
//            }
//            pool.clear();
//        } finally {
//            poolLock.unlock();
//        }
//    }
}
