package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class PlayBackResumed {
    public PlayBackResumed() {
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
