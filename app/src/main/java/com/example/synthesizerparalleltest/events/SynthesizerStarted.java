package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class SynthesizerStarted {
    public String interactionId;

    public SynthesizerStarted(String interactionId) {
        this.interactionId = interactionId;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
