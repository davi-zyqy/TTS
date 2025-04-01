package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class SynthesizerCompleted {
    public String interactionId;
    public long fbl;

    public SynthesizerCompleted(String interactionId) {
        this(interactionId, -1);
    }

    public SynthesizerCompleted(String interactionId, long fbl) {
        this.interactionId = interactionId; this.fbl = fbl;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
