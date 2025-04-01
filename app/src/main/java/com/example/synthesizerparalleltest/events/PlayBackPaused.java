package com.example.synthesizerparalleltest.events;

import com.google.gson.Gson;

public class PlayBackPaused {
    public PlayBackPaused() {
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
