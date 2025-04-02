//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.example.synthesizerparalleltest.ori;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.example.synthesizerparalleltest.config.TTSConfig;
import com.example.synthesizerparalleltest.config.TTSPolicy;
import com.example.synthesizerparalleltest.events.PlayBackPaused;
import com.example.synthesizerparalleltest.events.PlayBackResumed;
import com.example.synthesizerparalleltest.events.PlayCompleted;
import com.example.synthesizerparalleltest.events.PlayStarted;
import com.microsoft.cognitiveservices.speech.AudioDataStream;
import com.microsoft.cognitiveservices.speech.CancellationErrorCode;
import com.microsoft.cognitiveservices.speech.Connection;
import com.microsoft.cognitiveservices.speech.Diagnostics;
import com.microsoft.cognitiveservices.speech.EmbeddedSpeechConfig;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisWordBoundaryEventArgs;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.StreamStatus;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream;

public class Synthesizer {

    private enum ENTRYTYPE{
        INPUT_STREAM,
        PULL_AUDIO_OUTPUT_STREAM,
        STRING
    }

    // The state of the synthesizer
    private enum SynthesizerState{
        PLAYING,
        CANCELLING,
        PAUSED,
        STOPPED,
    }
    private class StreamEntry{
        public String interactionId;
        public ENTRYTYPE entryType;
        public Runnable onStarted;
        public Runnable onCompleted;
        public Runnable onCancelled;
        public String text;
        public long fbl;
        public PullAudioOutputStream pullAudioOutputStream;
        public InputStream inputStream;
        public AudioDataStream audioDataStream;
        public byte[] bytes;
        public AudioAttributes audioAttributes;

        public StreamEntry(PullAudioOutputStream pullAudioOutputStream, String interactionId, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
            this.pullAudioOutputStream = pullAudioOutputStream;
            this.interactionId = interactionId;
            this.entryType = ENTRYTYPE.PULL_AUDIO_OUTPUT_STREAM;
            this.onStarted = onStarted;
            this.onCompleted = onCompleted;
            this.onCancelled = onCanceled;
            this.audioAttributes = audioAttributes;
        }

        public StreamEntry(InputStream stream, String interactionId, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
            this.inputStream = stream;
            this.interactionId = interactionId;
            this.entryType = ENTRYTYPE.INPUT_STREAM;
            this.onStarted = onStarted;
            this.onCompleted = onCompleted;
            this.onCancelled = onCanceled;
            this.audioAttributes = audioAttributes;
        }

        public StreamEntry(String content, AudioAttributes audioAttributes){
            this.text = content;
            this.entryType = ENTRYTYPE.STRING;
            this.audioAttributes = audioAttributes;
        }

        public StreamEntry(String content, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
            this.text = content;
            this.entryType = ENTRYTYPE.STRING;
            this.onStarted = onStarted;
            this.onCompleted = onCompleted;
            this.onCancelled = onCanceled;
            this.audioAttributes = audioAttributes;
        }

        public StreamEntry(String content, String interactionId, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
            this.text = content;
            this.interactionId = interactionId;
            this.entryType = ENTRYTYPE.STRING;
            this.onStarted = onStarted;
            this.onCompleted = onCompleted;
            this.onCancelled = onCanceled;
            this.audioAttributes = audioAttributes;
        }

        public void startSynthesizing() throws ExecutionException, InterruptedException {
            if(entryType == ENTRYTYPE.STRING){
                this.audioDataStream = AudioDataStream.fromResult(speechSynthesizer.StartSpeakingSsmlAsync(this.text).get());

            }
            else{
                Log.w(LOGTAG, "startSynthesizing: entryType is not string");
            }
        }

        public long read(byte[] buffer) throws IOException {
            switch (entryType){
                case INPUT_STREAM:
                    return inputStream.read(buffer);
                case PULL_AUDIO_OUTPUT_STREAM:
                    return pullAudioOutputStream.read(buffer);
                case STRING:
                    return audioDataStream.readData(buffer);
                default:
                    return 0;
            }
        }

        public void close() throws IOException {
            switch (entryType){
                case INPUT_STREAM:
                    inputStream.close();
                    break;
                case PULL_AUDIO_OUTPUT_STREAM:
                    pullAudioOutputStream.close();
                    break;
                case STRING:
                    audioDataStream.close();
                    break;
                default:
                    break;
            }
        }
    }

    static final String LOGTAG = "Synthesizer";

    private final Context context;

    private AudioTrack audioTrack;
    final int SAMPLE_RATE = 24000;
    static final int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // The state of the synthesizer
    // Should ensure it thread safe.
    private SynthesizerState synthesizerStatus = SynthesizerState.STOPPED;
    private int playBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfiguration, audioEncoding);
    private LinkedList<StreamEntry> streamList ;
    private Lock streamListLock = new ReentrantLock();

    private String offlineVoiceName;
    private String offLineVoiceKey;
    private String ssmlPattern;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer speechSynthesizer;
    private Connection connection;
    private final Object synchronizedObj = new Object();

    // Whether to enable retry on timeout. If the network is unstable, and disconnects when synthesizing.
    // We can get the reading progress and retry to synthesize the rest of the text.
    private final boolean retryOnTimeout = true;
    private final ArrayList<SpeechSynthesisWordBoundaryEventArgs> wordBoundaries = new ArrayList<>();

    public boolean dumpTtsStream = false;
    public FileOutputStream ttsOutputStream = null;

    private TTSConfig ttsConfig;

    // A dictionary to store marker position for the end of each record stream.
    // The key is the value of the marker position. the value is the interaction id.
    private HashMap<Integer, String> markerPositionMap = new HashMap<>();
    private Queue<StreamEntry> playCompletedQueue = new LinkedList<>();
    private static final int startMarkerPosition = 1;

    // frameSizeInBytes = (channelCount * bitsPerSample) / 8
    // bitsPerSample = 16 (ENCODING_PCM_16BIT)
    // channelCount = 1 (CHANNEL_OUT_MONO)
    // frameSizeInBytes = 16 * 1 / 8 = 2
    private static final int frameSizeInBytes = 2;

    private int totalAudioDataInBytes = 0;

    private AudioAttributes currentAudioAttributes;

    private byte[] emptyAudio = new byte[1200 * frameSizeInBytes];

    public Synthesizer(Context context, TTSConfig ttsConfig, int asid, boolean isDump) {
        Log.i(LOGTAG, "TTSConfig: " + ttsConfig);

        if (isDump) {
            dumpTtsStream = true;
        }

        this.ttsConfig = ttsConfig;
        //this.audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        this.streamList = new LinkedList<StreamEntry>();
        this.offlineVoiceName = this.ttsConfig.ttsVoiceName;

        if (this.ttsConfig.ttsModelKey != null) {
            this.offLineVoiceKey = this.ttsConfig.ttsModelKey;
        }

        this.ssmlPattern = this.ttsConfig.ttsSsmlPattern;

        this.context = context;
        if (speechSynthesizer != null) {
            connection.close();
            speechSynthesizer.close();
            speechConfig.close();
        }

        speechConfig = SpeechConfig.fromSubscription(this.ttsConfig.ttsKey, this.ttsConfig.ttsRegion);
        // use 24k Hz format for higher quality
        speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm);

        // use this line to disable compression format in transmission.
        // speechConfig.setProperty("SpeechServiceConnection_SynthEnableCompressedAudioTransmission", "false");

        speechConfig.setProperty("SpeechSynthesis_KeepConnectionAfterStopping", "true");
        if (offLineVoiceKey != null) {
            speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthModelKey, offLineVoiceKey);
        }

        // The variable is not used, but is required to initialize some static variables.

        if (this.ttsConfig.ttsPolicy == TTSPolicy.ONLINE) {
            EmbeddedSpeechConfig embeddedSpeechConfig = EmbeddedSpeechConfig.fromPath(this.context.getExternalFilesDir("/TTSModel").toString());
            speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthBackend, "online");
        }
        else {
            String offlineDataPath = this.ttsConfig.ttsModelPath;
            EmbeddedSpeechConfig embeddedSpeechConfig = EmbeddedSpeechConfig.fromPath(offlineDataPath);
            speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthBackend, "hybrid");
            // set offline model path. For example: /sdcard/tts/XiaoxiaoDeviceV1
            speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthOfflineDataPath, offlineDataPath);
            // Set the offline voice name. For example: "Microsoft Server Speech Text to Speech Voice (zh-CN, Xiaoxiao)"
            // This is only required if your offline model path contains multiple voices.
            // Check the token.xml in offline voice folder for the voice name
            //speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthOfflineVoice, yourOfflineVoiceName);
            if (this.offlineVoiceName != null && !this.offlineVoiceName.isEmpty()) {
                speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthOfflineVoice, this.offlineVoiceName);
            }
            if (this.ttsConfig.ttsModelKey != null && !this.ttsConfig.ttsModelKey.isEmpty()) {
                speechConfig.setProperty(PropertyId.SpeechServiceConnection_SynthModelKey, this.ttsConfig.ttsModelKey);
            }
            if (this.ttsConfig.ttsPolicy == TTSPolicy.OFFLINE) {
                speechConfig.setProperty("SPEECH-SynthBackendSwitchingPolicy", "force_offline");
            }
            else {
                speechConfig.setProperty("SPEECH-SynthBackendSwitchingPolicy", "parallel_buffer");
                speechConfig.setProperty("SPEECH-SynthBackendFallbackBufferTimeoutMs", "" + this.ttsConfig.backendFallbackBufferTimeoutInMS);
                speechConfig.setProperty("SPEECH-SynthBackendFallbackBufferLengthMs", "" + this.ttsConfig.backendFallbackBufferLengthInMS);
            }
        }

        // Log method #1: write log to memory, and dump recent 10000 lines log to file when needed
        // e.g. synthesis canceled, exception caught, or crashed.
        Diagnostics.startMemoryLogging();
        // log method #2, save all logs into file. This will save all log to one file, disable this in production.
        if (this.ttsConfig.logFilePath != null && !this.ttsConfig.logFilePath.isEmpty()) {
            speechConfig.setProperty(PropertyId.Speech_LogFilename, this.ttsConfig.logFilePath);
        }
        // Log method #3: redirect logs to logcat
        //Diagnostics.startConsoleLogging();

        // Enable caching. For now, the SDK online cache SSML (not plain text) and online result.
        if (this.ttsConfig.cachePath != null && !this.ttsConfig.cachePath.isEmpty()) {
            speechConfig.setProperty("SPEECH-SynthesisCachingPath", this.ttsConfig.cachePath);
        }
        // Change this number to update max caching number.
        //speechConfig.setProperty("SPEECH-SynthesisCachingMaxNumber", "200");
        if (this.ttsConfig.cachingMaxNumber > 0) {
            speechConfig.setProperty("SPEECH-SynthesisCachingMaxNumber", "" + this.ttsConfig.cachingMaxNumber);
        }
        // To limit cache capacity, SPEECH-SynthesisCachingMaxNumber need to be set firstly
        // Change this number to limit total cache capacity, only for specific purpose.
        //speechConfig.setProperty("SPEECH-SynthesisCachingCapacityInBytes", "10000");
        if (this.ttsConfig.cachingCapacityInBytes > 0) {
            speechConfig.setProperty("SPEECH-SynthesisCachingCapacityInBytes", "" + this.ttsConfig.cachingCapacityInBytes);
        }
        // Change this number to limit max cache capacity of one item, only for specific purpose.
        //speechConfig.setProperty("SPEECH-SynthesisCachingMaxSizeOfOneItemInBytes", "10000");
        if (this.ttsConfig.cachingMaxSizeOfOneItemInBytes > 0) {
            speechConfig.setProperty("SPEECH-SynthesisCachingMaxSizeOfOneItemInBytes", "" + this.ttsConfig.cachingMaxSizeOfOneItemInBytes);
        }

        try{
            speechSynthesizer = new SpeechSynthesizer( speechConfig, null);
        }
        catch (Exception ex) {
            Log.e(LOGTAG, "unexpected " + ex.getMessage());
            ex.printStackTrace();
            assert(false);
        }
        // This method could pre-establish the connection to service to lower the latency
        connection = Connection.fromSpeechSynthesizer(speechSynthesizer);

        connection.connected.addEventListener((o, e) -> {
            Log.i(LOGTAG, "Connection established.");
        });

        connection.disconnected.addEventListener((o, e) -> {
            Log.i(LOGTAG, "Disconnected.");
        });

        connection.openConnection(true);

        speechSynthesizer.SynthesisStarted.addEventListener((o, e) -> {
            Log.i(LOGTAG, String.format(
                    "Synthesis started. Result Id: %s.",
                    e.getResult().getResultId()));
            e.close();
        });

        speechSynthesizer.Synthesizing.addEventListener((o, e) -> {
            // Indicates which backend the synthesis is finished by. Online or Offline.
            String backend = e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_SynthesisBackend);
            Log.i(LOGTAG, String.format(
                    "Synthesizing. received %d bytes. Finished by: %s.",
                    e.getResult().getAudioLength(),
                    backend));
            e.close();
        });

        speechSynthesizer.SynthesisCompleted.addEventListener((o, e) -> {
//            SpeechSynthesizer syn = (SpeechSynthesizer) o;
//
////            syn.
//            SpeechSynthesisResult result = e.getResult();
//            result.

            Log.i(LOGTAG, "Synthesis finished. Result Id: " + e.getResult().getResultId());
            Log.i(LOGTAG, "First byte latency: " + e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_SynthesisFirstByteLatencyMs) + " ms.");
            Log.i(LOGTAG, "Last byte latency: " + e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_SynthesisFinishLatencyMs) + " ms.");
            Log.i(LOGTAG, "Audio duration: " + e.getResult().getAudioLength() * 1000 / 24000 / 2 + " ms.");
            e.close();
        });

        speechSynthesizer.SynthesisCanceled.addEventListener((o, e) -> {
            String cancellationDetails =
                    SpeechSynthesisCancellationDetails.fromResult(e.getResult()).toString();
            Log.i(LOGTAG, "Error synthesizing. Result ID: " + e.getResult().getResultId() +
                            ". Error detail: " + System.lineSeparator() + cancellationDetails);
            e.close();
        });

        AudioAttributes attributes = this.ttsConfig.audioTrackAudioAttributes;
        if (attributes == null) {
            currentAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            attributes = currentAudioAttributes;
        }
        else {
            currentAudioAttributes = attributes;
        }

        createAudioTrack(attributes);

        Log.v("LOGTAG", "Opening connection.\n");
    }

    private void createAudioTrack(AudioAttributes attributes) {
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT)*2)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        totalAudioDataInBytes = 0;
        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.i(LOGTAG, "MarkedReached: " + track.getPlaybackHeadPosition());
                StreamEntry entry = playCompletedQueue.poll();
                if(entry ==null){
                    Log.i(LOGTAG, "MarkedReached: entry is null.");
                    return;
                }
                Log.i(LOGTAG, "MarkedReached: Play completed for ID: " + entry.interactionId);
                EventBus.getDefault().post(new PlayCompleted(entry.interactionId));
                new Thread(() -> {
                    playNext();
                }).start();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }
        });
    }

    public void playStream(String content, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
        String ssml = tryGenerateSsml(content);
        playStreamEntry(new StreamEntry(ssml, audioAttributes, onStarted, onCompleted, onCanceled));
    }

    public void playStream(String content, String interactionId, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
        String ssml = tryGenerateSsml(content);
        playStreamEntry(new StreamEntry(ssml, interactionId, audioAttributes, onStarted, onCompleted, onCanceled));
    }

    public void playStream(String content, AudioAttributes audioAttributes){
        String ssml = tryGenerateSsml(content);
        playStreamEntry(new StreamEntry(ssml, audioAttributes));
    }

    public void playStream(PullAudioOutputStream stream, String interactionId, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
        playStreamEntry(new StreamEntry(stream, interactionId, audioAttributes, onStarted, onCompleted, onCanceled));
    }

    public void playStream(InputStream inputStream, String interactionId, AudioAttributes audioAttributes, Runnable onStarted, Runnable onCompleted, Runnable onCanceled){
        playStreamEntry(new StreamEntry(inputStream, interactionId, audioAttributes, onStarted, onCompleted, onCanceled));
    }

    // This method is used to cancel the current playing stream.
    // It will stop the current playing stream and clear the stream list.
    // If the synthesizer is in cancelling state, we can not play new stream until state is changed to stopped.
    public void cancelPlayback(){
        // If the synthesizer is not playing or paused, there is nothing to cancel.
        if(synthesizerStatus != SynthesizerState.PLAYING && synthesizerStatus != SynthesizerState.PAUSED){
            Log.i(LOGTAG, "There is nothing to cancel");
            return;
        }

        if(this.audioTrack==null){
            Log.i(LOGTAG, "Audio track is not initialized.");
            return;
        }

        synchronized (synchronizedObj) {
            synthesizerStatus = SynthesizerState.CANCELLING;
        }

        audioTrack.setNotificationMarkerPosition(0);
        audioTrack.pause();
        audioTrack.flush();

        Log.i(LOGTAG, "Cancelling playback");
    }

    public void pauseTTS() {
        Log.i(LOGTAG, "Pause TTS");
        if(this.audioTrack==null){
            Log.i(LOGTAG, "Audio track is not initialized.");
            return;
        }

        // If the synthesizer is not playing, there is nothing to pause.
        if(synthesizerStatus != SynthesizerState.PLAYING){
            Log.i(LOGTAG, "Audio track is not playing.");
            return;
        }

        synchronized (synchronizedObj) {
            synthesizerStatus = SynthesizerState.PAUSED;
        }
        this.audioTrack.pause();
        EventBus.getDefault().post(new PlayBackPaused());
    }

    public void resumeTTS() {
        Log.i(LOGTAG, "Resume TTS");
        if(this.audioTrack==null){
            Log.i(LOGTAG, "Audio track is not initialized.");
            return;
        }

        // If the synthesizer is not paused, there is nothing to resume.
        if(synthesizerStatus != SynthesizerState.PAUSED){
            Log.i(LOGTAG, "Audio track is not paused.");
            return;
        }

        this.audioTrack.play();
        synchronized (synchronizedObj) {
            synthesizerStatus = SynthesizerState.PLAYING;
        }
        EventBus.getDefault().post(new PlayBackResumed());
    }

    // This method is used to play stream entry.
    private void playStreamEntry(StreamEntry entry) {
        Log.i(LOGTAG, "playStreamEntry: " + entry.interactionId);
        // If the synthesizer is in paused state, we need to cancel the current playing stream.
        if(synthesizerStatus == SynthesizerState.PAUSED){
            cancelPlayback();
        }
        // If the synthesizer is in cancelling state, then we need to wait for it to be stopped.
        try{
            while(synthesizerStatus == SynthesizerState.CANCELLING){
                Thread.sleep(100);
            }
        }
        catch (InterruptedException e) {
            Log.w(LOGTAG, "Failed to wait for synthesizer to be stopped.");
        }

        switch (synthesizerStatus){
            case PLAYING:
                safeAddToStreamList(entry);
                break;
            case STOPPED:
                safeAddToStreamList(entry);
                startPlaying();
                break;
            default:
                Log.w(LOGTAG, "Invalid synthesizer state.");
                break;
        }
    }

    private void playNext() {
        Log.i(LOGTAG, "Left Count: " + streamList.size());
        if (synthesizerStatus == SynthesizerState.STOPPED) {
            startPlaying();
        }
    }

    private void startPlaying(){
        synchronized (synchronizedObj) {
            synthesizerStatus = SynthesizerState.PLAYING;
        }
        Log.d(LOGTAG, "Speech Synthesis Start");

        new Thread() {
            byte[] buffer = new byte[playBufSize];

            public void run() {
                // Play the stream list until it is empty.
                while (streamList.size() > 0){
                    try {
                        if(streamList.peekFirst() != null) {
                            StreamEntry entry = streamList.getFirst();
                            Long startTime = System.currentTimeMillis();

                            if (entry.audioAttributes != null && currentAudioAttributes != entry.audioAttributes) {
                                Log.i(LOGTAG, "recreate audio track");
                                audioTrack.release();
                                currentAudioAttributes = entry.audioAttributes;
                                createAudioTrack(entry.audioAttributes);
                            }
                            audioTrack.stop();
                            audioTrack.setNotificationMarkerPosition(0);
                            audioTrack.play();
                            Log.i(LOGTAG, "Audio Track Position: " + audioTrack.getPlaybackHeadPosition());
                            new Thread(() -> {
                                if (entry.onStarted != null) entry.onStarted.run();
                            }).start();

                            wordBoundaries.clear();
                            if(entry.entryType == ENTRYTYPE.STRING){
                                entry.startSynthesizing();
                            }
                            boolean first = true;
                            if (dumpTtsStream) {
                                createTtsOutputStream();
                            }

                            // Write streaming data from TTS result to audio track.
                            while (synthesizerStatus != SynthesizerState.CANCELLING) {
                                if(synthesizerStatus != SynthesizerState.PAUSED){
                                    long len = entry.read(buffer);
                                    if (len <= 0) {
                                        // End of stream.
                                        // len will be 0 for audioDataStream and -1 for inputStream.
                                        // Set the marker position to the end of the stream.
                                        Log.i(LOGTAG, "end of stream. Position: " + audioTrack.getPlaybackHeadPosition() + " totalAudioDataInFrames: " + totalAudioDataInBytes/frameSizeInBytes);
                                        playCompletedQueue.offer(entry);
                                        totalAudioDataInBytes += emptyAudio.length;
                                        int marker = totalAudioDataInBytes/frameSizeInBytes;
                                        Log.i(LOGTAG, "setNotificationMarkerPosition: " + marker);
                                        audioTrack.setNotificationMarkerPosition(marker);
                                        audioTrack.write(emptyAudio, 0, emptyAudio.length);
                                        break;
                                    }

                                    Log.i(LOGTAG, "Read data len: " + len);
                                    if (first) {
                                        // Record time to get the first data and post play start event.
                                        first = false;
                                        Long latency = System.currentTimeMillis() - startTime;
                                        Log.d(LOGTAG, "Speech Synthesis Got first data latency: " + latency);
                                        entry.fbl = latency;
                                        EventBus.getDefault().post(new PlayStarted(entry.interactionId));
                                    }

                                    int writeLen = audioTrack.write(buffer, 0, (int) len);
                                    dumpTtsOutputStream(buffer);
                                    totalAudioDataInBytes += writeLen;
                                }
                                else{
                                    // Wait until audio is un-paused.
                                    Thread.sleep(100);
                                }
                            }
                            if( entry.entryType == ENTRYTYPE.STRING){
                                // If the synthesizer connection failed or the service timed out, retry the synthesis.
                                if (synthesizerStatus != SynthesizerState.CANCELLING && entry.audioDataStream.getStatus() == StreamStatus.Canceled && retryOnTimeout) {
                                    SpeechSynthesisCancellationDetails details = SpeechSynthesisCancellationDetails.fromStream(entry.audioDataStream);
                                    CancellationErrorCode code = details.getErrorCode();
                                    Log.w(LOGTAG, "Synthesis canceled, error code" + details.getErrorCode() + "; details:" + details.getErrorDetails());

                                    if (entry.audioDataStream.getPosition() > 0 && (code == CancellationErrorCode.ConnectionFailure || code == CancellationErrorCode.ServiceTimeout)) {
                                        Log.w(LOGTAG, "Synthesis retrying.");
                                        RetrySynthesis(entry.audioDataStream, entry);
                                    }
                                }
                            }

                            entry.close();
                            closeTtsOutputStream();
                        }

                        // If the synthesizer is canceled, cancel and empty the stream list.
                        if(synthesizerStatus == SynthesizerState.CANCELLING){
                            playCompletedQueue.clear();
                            safeEmptyStreamList();
                            break;
                        }
                        // Remove the first processed stream from the list.
                        streamListLock.lock();
                        try{
                            if(streamList.size() > 0) {
                                StreamEntry entry = streamList.removeFirst();
                                if (entry.onCompleted != null) entry.onCompleted.run();
                            }
                        }
                        finally {
                            streamListLock.unlock();
                        }
                    } catch (Exception e) {
                        Log.i(LOGTAG, "read exception", e);
                        break;
                    }

                    break;
                }

                // playback may still be in progress, need to wait for it to finish.
                // TODO: refine it!
                /*
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                */
                //audioTrack.stop();

                synchronized (synchronizedObj) {
                    synthesizerStatus = SynthesizerState.STOPPED;
                    totalAudioDataInBytes = 0;
                }
                Log.d(LOGTAG, "Speech Synthesis End");
            }
        }.start();
    }

    // Retry the synthesis with the remaining text.
    private void RetrySynthesis(AudioDataStream audioDataStream, StreamEntry entry) throws ExecutionException, InterruptedException {
        long duration = audioDataStream.getPosition() / 48; // milliseconds for 24kHz
        long textStart = 0;
        long textEnd = 0;
        String ssml = entry.text;

        // Find the last word boundary before the timeout.
        if (wordBoundaries != null && !wordBoundaries.isEmpty()) {
            textStart = wordBoundaries.get(0).getTextOffset();
            textEnd = wordBoundaries.get(0).getTextOffset() + wordBoundaries.get(0).getWordLength();
            for (SpeechSynthesisWordBoundaryEventArgs e : wordBoundaries) {
                if (e.getAudioOffset() / 10 / 1000 > duration) {
                    break;
                }
                textEnd = e.getTextOffset() + e.getWordLength();
            }
        }

        // Removed already spoken text from SSML
        String remainedSsml = ssml.substring(0, (int) textStart) + ssml.substring((int) textEnd);
        Log.d(LOGTAG, "Retry SSML: " + remainedSsml);

        // Retry with the remaining text
        byte[] buffer = new byte[playBufSize];
        SpeechSynthesisResult newSynthesisResult = speechSynthesizer.StartSpeakingSsmlAsync(remainedSsml).get();
        AudioDataStream newAudioDataStream = AudioDataStream.fromResult(newSynthesisResult);
        // !!! paused
        while (synthesizerStatus!= SynthesizerState.CANCELLING) {
            if (synthesizerStatus != SynthesizerState.PAUSED) {
                long len = newAudioDataStream.readData(buffer);
                if (len == 0) {
                    // End of stream.
                    // Set the marker position to the end of the stream.
                    Log.i(LOGTAG, "end of stream. Position: " + audioTrack.getPlaybackHeadPosition() + " totalAudioDataInFrames: " + totalAudioDataInBytes / frameSizeInBytes);
                    playCompletedQueue.offer(entry);
                    totalAudioDataInBytes += emptyAudio.length;
                    int marker = totalAudioDataInBytes/frameSizeInBytes;
                    Log.i(LOGTAG, "setNotificationMarkerPosition: " + marker);
                    audioTrack.setNotificationMarkerPosition(marker);
                    audioTrack.write(emptyAudio, 0, emptyAudio.length);
                    break;
                }

                int writeLen = audioTrack.write(buffer, 0, (int) len);
                dumpTtsOutputStream(buffer);
                totalAudioDataInBytes += writeLen;
            } else {
                // Wait until audio is un-paused.
                Thread.sleep(100);
            }
        }
        newAudioDataStream.close();
    }

    private void safeAddToStreamList(StreamEntry stream){
        this.streamListLock.lock();
        try {
            this.streamList.add(stream);
        } finally {
            this.streamListLock.unlock();
        }
    }

    private void safeEmptyStreamList(){
        this.streamListLock.lock();
        try{
            while (streamList.size() > 0) {
                StreamEntry entry = this.streamList.removeFirst();
                if(entry.onCancelled != null) entry.onCancelled.run();
            }

        } // we need to catch here to make sure an exception from one onCanceled doesn't stop the thread
        catch (Exception e){
            Log.w(LOGTAG, "onCanceled failed " + e.getMessage());
        }
        finally {
            this.streamListLock.unlock();
        }
    }

    private void dumpTtsOutputStream(byte[] data) {
        if (ttsOutputStream != null) {
            try {
                ttsOutputStream.write(data);
            }
            catch (Exception ex) {
                Log.w(LOGTAG, "Exception when dump tts output stream: " + ex.getMessage());
            }
        }
    }

    private void createTtsOutputStream() {
        String fileName = "TTS" + "-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".pcm";
        String filePath = this.context.getExternalFilesDir("/Dumps") + "/" + fileName;
//        String filePath = "/sdcard/dumps/" + fileName;
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirCreated = parentDir.mkdirs();
            if (!dirCreated) {
                Log.e("Synthesizer", "Failed to create directory: " + parentDir.getAbsolutePath());
                return;
            }
        }
        if (!file.exists()) {
            try {
                boolean fileCreated = file.createNewFile();
                if (!fileCreated) {
                    Log.e("Synthesizer", "Failed to create file: " + file.getAbsolutePath());
                    return;
                }
            } catch (IOException e) {
                Log.e("Synthesizer", "Exception while creating file: " + e.getMessage());
                return;
            }
        }
        try {
            ttsOutputStream = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void closeTtsOutputStream() {
        if (ttsOutputStream != null) {
            try {
                ttsOutputStream.flush();
                ttsOutputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String tryGenerateSsml(String content) {
        String ssml = content;
        // Use regex to check ssml format: "<speak[\S\s]>[\S\s]?</speak>"
        // If the content is not ssml format, then add ssml format to it.
        if (!content.matches("<speak[\\S\\s]*?>[\\S\\s]*?</speak>")) {
            ssml = String.format(this.ssmlPattern, content);
        }
        return ssml;
    }
}
