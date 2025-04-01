package com.example.synthesizerparalleltest.config;

import android.media.AudioAttributes;

import com.google.gson.Gson;

public class TTSConfig {
    public String slid;
    public String ttsModelPath;
    public String ttsVoiceName;
    public String ttsModelKey;
    public String ttsSsmlPattern;
    public String ttsKey;
    public String ttsRegion;
    public TTSPolicy ttsPolicy;
    public String cachePath;
    public int backendFallbackBufferTimeoutInMS;
    public int backendFallbackBufferLengthInMS;
    public String logFilePath;

    public int cachingMaxNumber;
    public int cachingCapacityInBytes;
    public int cachingMaxSizeOfOneItemInBytes;
    public AudioAttributes audioTrackAudioAttributes;

    public String toString() {
        return (new Gson()).toJson(this);
    }

    private TTSConfig(Builder builder) {
        this.slid = builder.slid;
        this.ttsModelPath = builder.ttsModelPath;
        this.ttsVoiceName = builder.ttsVoiceName;
        this.ttsModelKey = builder.ttsModelKey;
        this.ttsSsmlPattern = builder.ttsSsmlPattern;
        this.ttsKey = builder.ttsKey;
        this.ttsRegion = builder.ttsRegion;
        this.ttsPolicy = builder.ttsPolicy;
        this.cachePath = builder.cachePath;
        this.backendFallbackBufferTimeoutInMS = builder.backendFallbackBufferTimeoutInMS;
        this.backendFallbackBufferLengthInMS = builder.backendFallbackBufferLengthInMS;
        this.logFilePath = builder.logFilePath;
        this.cachingMaxNumber = builder.cachingMaxNumber;
        this.cachingCapacityInBytes = builder.cachingCapacityInBytes;
        this.cachingMaxSizeOfOneItemInBytes = builder.cachingMaxSizeOfOneItemInBytes;
        this.audioTrackAudioAttributes = builder.audioTrackAudioAttributes;
    }

    public static final class Builder {
        private String slid;
        private String ttsModelPath;
        private String ttsVoiceName;
        private String ttsModelKey;
        private String ttsSsmlPattern;
        private String ttsKey;
        private String ttsRegion;
        private TTSPolicy ttsPolicy;
        private String cachePath;
        private int backendFallbackBufferTimeoutInMS = 800;
        private int backendFallbackBufferLengthInMS = 200;
        private String logFilePath;
        private int cachingMaxNumber = 500;
        private int cachingCapacityInBytes = 0;
        private int cachingMaxSizeOfOneItemInBytes = 0;
        private AudioAttributes audioTrackAudioAttributes;

        public Builder() {
        }

        public Builder slid(String slid) {
            this.slid = slid;
            return this;
        }

        public Builder ttsModelPath(String ttsModelPath) {
            this.ttsModelPath = ttsModelPath;
            return this;
        }

        public Builder ttsVoiceName(String ttsVoiceName) {
            this.ttsVoiceName = ttsVoiceName;
            return this;
        }

        public Builder ttsModelKey(String ttsModelKey) {
            this.ttsModelKey = ttsModelKey;
            return this;
        }

        public Builder ttsSsmlPattern(String ttsSsmlPattern) {
            this.ttsSsmlPattern = ttsSsmlPattern;
            return this;
        }

        public Builder ttsKey(String ttsKey) {
            this.ttsKey = ttsKey;
            return this;
        }

        public Builder ttsRegion(String ttsRegion) {
            this.ttsRegion = ttsRegion;
            return this;
        }

        public Builder ttsPolicy(TTSPolicy ttsPolicy) {
            this.ttsPolicy = ttsPolicy;
            return this;
        }

        public Builder cachePath(String cachePath) {
            this.cachePath = cachePath;
            return this;
        }

        public Builder backendFallbackBufferTimeoutInMS(int backendFallbackBufferTimeoutInMS) {
            this.backendFallbackBufferTimeoutInMS = backendFallbackBufferTimeoutInMS;
            return this;
        }

        public Builder backendFallbackBufferLengthInMS(int backendFallbackBufferLengthInMS) {
            this.backendFallbackBufferLengthInMS = backendFallbackBufferLengthInMS;
            return this;
        }

        public Builder logFilePath(String logFilePath) {
            this.logFilePath = logFilePath;
            return this;
        }

        public Builder cachingMaxNumber(int cachingMaxNumber) {
            this.cachingMaxNumber = cachingMaxNumber;
            return this;
        }

        public Builder cachingCapacityInBytes(int cachingCapacityInBytes) {
            this.cachingCapacityInBytes = cachingCapacityInBytes;
            return this;
        }

        public Builder cachingMaxSizeOfOneItemInBytes(int cachingMaxSizeOfOneItemInBytes) {
            this.cachingMaxSizeOfOneItemInBytes = cachingMaxSizeOfOneItemInBytes;
            return this;
        }

        public Builder audioTrackAudioAttributes(AudioAttributes audioTrackAudioAttributes) {
            this.audioTrackAudioAttributes = audioTrackAudioAttributes;
            return this;
        }

        public TTSConfig build() {
            return new TTSConfig(this);
        }
    }
}
