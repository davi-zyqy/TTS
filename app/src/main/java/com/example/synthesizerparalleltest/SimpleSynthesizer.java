package com.example.synthesizerparalleltest;

import android.content.Context;
import android.media.AudioAttributes;
import android.util.Log;

import com.example.synthesizerparalleltest.config.TTSConfig;
import com.example.synthesizerparalleltest.config.TTSPolicy;
import com.example.synthesizerparalleltest.events.SynthesizerCompleted;
import com.microsoft.cognitiveservices.speech.AudioDataStream;
import com.microsoft.cognitiveservices.speech.Connection;
import com.microsoft.cognitiveservices.speech.Diagnostics;
import com.microsoft.cognitiveservices.speech.EmbeddedSpeechConfig;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class SimpleSynthesizer {
    static final String LOGTAG = "Synthesizer";

    private final Context context;
    private TTSConfig ttsConfig;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer speechSynthesizer;
    private Connection connection;
    private AudioAttributes currentAudioAttributes;
    public AudioDataStream audioDataStream;

    public Runnable onCompleted;
    public Runnable onCanceled;

    private String offlineVoiceName;
    private String offLineVoiceKey;
    private String ssmlPattern;
    private String ssml;

    public SimpleSynthesizer(Context context, TTSConfig ttsConfig) {
        this.context = context;
        this.ttsConfig = ttsConfig;
        this.ssmlPattern = this.ttsConfig.ttsSsmlPattern;

        if (this.ttsConfig.ttsModelKey != null) {
            this.offLineVoiceKey = this.ttsConfig.ttsModelKey;
        }

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
//            SpeechSynthesisResult result = e.getResult();
//            result.
//            String fileName = "TTS" + "-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".wav";
//            String filePath = this.context.getExternalFilesDir("/Dumps") + "/" + fileName;
//            this.audioDataStream.saveToWavFileAsync(filePath);

            Log.i(LOGTAG, "Synthesis finished. Result Id: " + e.getResult().getResultId());
            Log.i(LOGTAG, "First byte latency: " + e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_SynthesisFirstByteLatencyMs) + " ms.");
            Log.i(LOGTAG, "Last byte latency: " + e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_SynthesisFinishLatencyMs) + " ms.");
            Log.i(LOGTAG, "Audio duration: " + e.getResult().getAudioLength() * 1000 / 24000 / 2 + " ms.");
            e.close();

            alterToHybrid();

//            EventBus.getDefault().post(new SynthesizerCompleted("interactionId"));
            if (this.onCompleted != null) {
                this.onCompleted.run();
            }
        });

        speechSynthesizer.SynthesisCanceled.addEventListener((o, e) -> {
            String cancellationDetails =
                    SpeechSynthesisCancellationDetails.fromResult(e.getResult()).toString();
            Log.i(LOGTAG, "Error synthesizing. Result ID: " + e.getResult().getResultId() +
                    ". Error detail: " + System.lineSeparator() + cancellationDetails);
            e.close();

//            if (this.onCanceled != null) {
//                this.onCanceled.run();
//            }
            alterToOffline();
            reSynthesize();

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

//        createAudioTrack(attributes);

        Log.v("LOGTAG", "Opening connection.\n");
    }


    public void startSynthesizing(String text) throws ExecutionException, InterruptedException {
        if(!text.isEmpty()){
            this.ssml = tryGenerateSsml(text);
//            this.audioDataStream = AudioDataStream.fromResult(speechSynthesizer.StartSpeakingSsmlAsync(ssml).get());
            this.speechSynthesizer.StartSpeakingSsmlAsync(ssml);

        }
        else{
            Log.w(LOGTAG, "startSynthesizing: entryType is not string");
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

    public void setOnCompleted(Runnable onCompleted) {
        this.onCompleted = onCompleted;
    }

    public void setOnCanceled(Runnable onCanceled) {
        this.onCanceled = onCanceled;
    }

    private void reSynthesize() {
        if (this.ssml != null) {
            this.speechSynthesizer.StartSpeakingSsmlAsync(this.ssml);
        }

    }

    private void alterToOffline() {
//        Log.i(LOGTAG, "alter to offline");
//        this.speechConfig.setProperty("SPEECH-SynthBackendSwitchingPolicy", "force_offline");
        this.speechSynthesizer.getProperties().setProperty("SPEECH-SynthBackendSwitchingPolicy", "force_offline");
//        String offlineDataPath = this.ttsConfig.ttsModelPath;
//        this.speechSynthesizer.getProperties().setProperty(PropertyId.SpeechServiceConnection_SynthOfflineDataPath, offlineDataPath);
//        if (this.offlineVoiceName != null && !this.offlineVoiceName.isEmpty()) {
//            this.speechSynthesizer.getProperties().setProperty(PropertyId.SpeechServiceConnection_SynthOfflineVoice, this.offlineVoiceName);
//        }
//        if (this.ttsConfig.ttsModelKey != null && !this.ttsConfig.ttsModelKey.isEmpty()) {
//            this.speechSynthesizer.getProperties().setProperty(PropertyId.SpeechServiceConnection_SynthModelKey, this.ttsConfig.ttsModelKey);
//        }

        Log.i(LOGTAG, "alter to offline");
//        String a = speechSynthesizer.getProperties().getProperty("SPEECH-SynthBackendSwitchingPolicy");
//        Log.i(LOGTAG, offlineDataPath);
    }

    private void alterToHybrid() {
        this.speechSynthesizer.getProperties().setProperty("SPEECH-SynthBackendSwitchingPolicy", "parallel_buffer");
        this.speechSynthesizer.getProperties().setProperty("SPEECH-SynthBackendFallbackBufferTimeoutMs", "" + this.ttsConfig.backendFallbackBufferTimeoutInMS);
        this.speechSynthesizer.getProperties().setProperty("SPEECH-SynthBackendFallbackBufferLengthMs", "" + this.ttsConfig.backendFallbackBufferLengthInMS);
    }
}
