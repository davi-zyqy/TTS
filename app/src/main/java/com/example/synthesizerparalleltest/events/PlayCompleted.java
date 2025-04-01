package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class PlayCompleted {
    public String interactionId;
    public PlayCompleted(String interactionId) {
        this.interactionId = interactionId;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
