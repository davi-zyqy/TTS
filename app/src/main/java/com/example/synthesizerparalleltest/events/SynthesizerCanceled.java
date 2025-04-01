package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class SynthesizerCanceled {
    public String interactionId;
    public SynthesizerCanceled(String interactionId) {
        this.interactionId = interactionId;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
