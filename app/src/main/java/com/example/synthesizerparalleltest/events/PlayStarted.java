package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class PlayStarted {
    public String interactionId;
    public PlayStarted(String interactionId) {
        this.interactionId = interactionId;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
