package com.example.ballsortchallenge;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    // Game Constants
    static final int MAX_CAPACITY = 4;
    private static final String PREFS_NAME = "BallSortPrefs";
    private static final String KEY_LEVEL = "current_level";
    private static final String KEY_COINS = "user_coins";
    private static final String KEY_LAST_CLAIM = "last_claim_date";
    private static final String KEY_SOUND = "sound_setting";

    // Game State
    ArrayList<Stack<Integer>> tubes = new ArrayList<>();
    ArrayList<Stack<Integer>> initialTubes = new ArrayList<>();
    Stack<Move> undoStack = new Stack<>();
}

