package com.example.synthesizerparalleltest;

public interface OnCompletedListener {
    void run(String fileName, String resultId, String firstByteLatency, String lastByteLatency, String duration);
}
