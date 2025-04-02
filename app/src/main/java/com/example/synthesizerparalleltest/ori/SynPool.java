package com.example.synthesizerparalleltest.ori;

import android.content.Context;
import android.util.Log;

import com.example.synthesizerparalleltest.config.TTSConfig;
import com.example.synthesizerparalleltest.ori.Synthesizer;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SynPool {
    private static final String LOGTAG = "SynthesizerPool";
    private final LinkedList<Synthesizer> pool = new LinkedList<>();
//    private ThreadLocal<Synthesizer> currentSynthesizer = new ThreadLocal<>();
    private final Lock poolLock = new ReentrantLock();
    private final int maxPoolSize;
    private final TTSConfig baseConfig;
    private final Context context;
    private boolean isDump;

    public SynPool(Context context, TTSConfig ttsConfig, int poolSize, boolean isDump) {
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
            Synthesizer synthesizer = createNewSynthesizer();
            if (synthesizer != null) {
                pool.add(synthesizer);
            }
        }
    }

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
            return new Synthesizer(context, configCopy, -1, isDump);
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

    public boolean isSynthesizerAvailable() {
        return !pool.isEmpty();
    }



//    public Synthesizer borrowSynthesizer() {
//        if (pool.isEmpty()) {
//            return null;
//        }
//        try {
//            Synthesizer synthesizer = pool.take(); // 阻塞直到有可用对象
//            currentSynthesizer.set(synthesizer); // 设置当前线程的 Synthesizer
//            return synthesizer;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            return null;
//        }
//    }

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
//        pool.add(synthesizer);
//        currentSynthesizer.remove(); // 清除当前线程的 Synthesizer

//        Synthesizer synthesizer = currentSynthesizer.get();
//        if (synthesizer != null) {
//            try {
//                pool.put(synthesizer); // 归还到池中
//                currentSynthesizer.remove(); // 清除当前线程的 Synthesizer
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }

//        if (synthesizer == null) {
//            return;
//        }
//        poolLock.lock();
//        try {
//            if (pool.size() < maxPoolSize) {
//                pool.put(synthesizer);
//                currentSynthesizer.remove();
//            }
//        } finally {
//            poolLock.unlock();
//        }
    }

//    public Synthesizer getCurrentSynthesizer() {
//        return currentSynthesizer.get(); // 获取当前线程的 Synthesizer
//    }

//    @Subscribe(threadMode = ThreadMode.POSTING)
//    public void onPlayCompleted(PlayCompleted event) {
//        returnSynthesizer();
//    }
}
