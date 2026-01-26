package com.example.ballsortchallenge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

    int selectedTube = -1;
    int currentLevel = 1;
    int coins = 100;
    int undosLeft = 5;
    boolean isSoundOn = true;
    boolean hasPlayerMoved = false;

    // UI Components
    TubeAdapter adapter;
    TextView levelText, coinText, undoText;
    Button btnAddTube;
    ImageButton btnSoundToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLevel = prefs.getInt(KEY_LEVEL, 1);
        coins = prefs.getInt(KEY_COINS, 100);
        isSoundOn = prefs.getBoolean(KEY_SOUND, true);

        // Bind UI
        levelText = findViewById(R.id.levelText);
        coinText = findViewById(R.id.coinText);
        undoText = findViewById(R.id.undoText);
        btnAddTube = findViewById(R.id.btnAddTube);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);

        // Setup Sound Toggle
        updateSoundIcon();
        btnSoundToggle.setOnClickListener(v -> {
            isSoundOn = !isSoundOn;
            prefs.edit().putBoolean(KEY_SOUND, isSoundOn).apply();
            updateSoundIcon();
        });

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.tubeRecycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, calculateSpanCount()));

        // Start Game
        loadLevel(currentLevel);
        checkDailyReward();
        updateUI();

        adapter = new TubeAdapter(this, tubes, this::onTubeClicked);
        recyclerView.setAdapter(adapter);
    }

    private void updateUI() {
        if (coinText != null) coinText.setText("🪙 " + coins);
        if (undoText != null) undoText.setText("🔄 " + undosLeft);

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_COINS, coins);
        editor.apply();
    }

    private void updateSoundIcon() {
        if (isSoundOn) {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            btnSoundToggle.setColorFilter(Color.WHITE);
        } else {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode);
            btnSoundToggle.setColorFilter(Color.RED);
        }
    }

    private void checkDailyReward() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastClaim = prefs.getString(KEY_LAST_CLAIM, "");
        String today = DateFormat.getDateInstance().format(new Date());

        if (!lastClaim.equals(today)) {
            coins += 50;
            updateUI();
            prefs.edit().putString(KEY_LAST_CLAIM, today).apply();
            Toast.makeText(this, "Daily Reward: +50 Coins! 🎁", Toast.LENGTH_LONG).show();
        }
    }

    private void playSound(int resId) {
        if (!isSoundOn) return;
        try {
            MediaPlayer mp = MediaPlayer.create(this, resId);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void onTubeClicked(int index) {
        if (selectedTube == -1) {
            if (tubes.get(index).isEmpty()) return;
            selectedTube = index;
            adapter.setSelectedPosition(index);
            playSound(R.raw.pop);
        } else {
            if (selectedTube == index) {
                selectedTube = -1;
                adapter.setSelectedPosition(-1);
                return;
            }

            Stack<Integer> from = tubes.get(selectedTube);
            Stack<Integer> to = tubes.get(index);

            if (canMove(from, to)) {
                int ball = from.pop();
                to.push(ball);
                undoStack.push(new Move(selectedTube, index, ball));
                hasPlayerMoved = true;
                playSound(R.raw.pop);

                if (isGameWon()) {
                    playSound(R.raw.success);
                    currentLevel++;
                    coins += 25;
                    updateUI();
                    saveProgress();
                    showWinDialog();
                }
            }
            selectedTube = -1;
            adapter.setSelectedPosition(-1);
            adapter.notifyDataSetChanged();
        }
    }

    private boolean canMove(Stack<Integer> from, Stack<Integer> to) {
        return !from.isEmpty() && to.size() < MAX_CAPACITY &&
                (to.isEmpty() || from.peek().equals(to.peek()));
    }

    public void undoMove(View view) {
        if (undoStack.isEmpty()) return;

        if (undosLeft > 0) {
            undosLeft--;
            performUndo();
        } else if (coins >= 5) {
            coins -= 5;
            performUndo();
            Toast.makeText(this, "Undo purchased! (-5 🪙)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Not enough coins for Undo!", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }

    private void performUndo() {
        Move move = undoStack.pop();
        tubes.get(move.toTube).pop();
        tubes.get(move.fromTube).push(move.color);
        adapter.notifyDataSetChanged();
    }

    public void restartGame(View view) {
        tubes.clear();
        undoStack.clear();
        hasPlayerMoved = false;
        undosLeft = 5;
        for (Stack<Integer> savedStack : initialTubes) {
            Stack<Integer> newStack = new Stack<>();
            newStack.addAll(savedStack);
            tubes.add(newStack);
        }
        selectedTube = -1;
        updateUI();
        resetAddTubeButton();
        adapter.setSelectedPosition(-1);
        adapter.notifyDataSetChanged();
    }

    private void loadLevel(int level) {
        tubes.clear();
        initialTubes.clear();
        undoStack.clear();
        hasPlayerMoved = false;
        undosLeft = 5;
        levelText.setText("Level " + level);

        int colors = (level <= 3) ? 2 : (level <= 8) ? 3 : (level <= 15) ? 4 : (level <= 25) ? 5 : 6;
        int emptyTubes = (colors >= 4) ? 2 : 1;

        ArrayList<Integer> bucket = new ArrayList<>();
        for (int c = 1; c <= colors; c++) {
            for (int i = 0; i < MAX_CAPACITY; i++) bucket.add(c);
        }
        Collections.shuffle(bucket);

        for (int i = 0; i < (colors + emptyTubes); i++) tubes.add(new Stack<>());

        int ballIndex = 0;
        for (int i = 0; i < colors; i++) {
            for (int j = 0; j < MAX_CAPACITY; j++) {
                tubes.get(i).push(bucket.get(ballIndex++));
            }
        }

        for (Stack<Integer> s : tubes) {
            Stack<Integer> copy = new Stack<>();
            copy.addAll(s);
            initialTubes.add(copy);
        }
        resetAddTubeButton();
    }

    private void showWinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layout = getLayoutInflater().inflate(R.layout.dialog_win, null);
        builder.setView(layout).setCancelable(false);
        AlertDialog dialog = builder.create();

        TextView title = layout.findViewById(R.id.winTitle);
        title.setText("Level " + (currentLevel - 1) + " Clear!");

        layout.findViewById(R.id.btnNext).setOnClickListener(v -> {
            loadLevel(currentLevel);
            adapter.notifyDataSetChanged();
            updateUI();
            dialog.dismiss();
        });
        dialog.show();
    }

    private boolean isGameWon() {
        if (!hasPlayerMoved) return false;
        int totalBalls = 0;
        for(Stack<Integer> tube : tubes) totalBalls += tube.size();
        int expectedCompleted = totalBalls / MAX_CAPACITY;
        int completed = 0;

        for (Stack<Integer> tube : tubes) {
            if (tube.isEmpty()) continue;
            if (tube.size() == MAX_CAPACITY) {
                int color = tube.get(0);
                boolean allSame = true;
                for (int ball : tube) if (ball != color) { allSame = false; break; }
                if (allSame) completed++;
            } else return false;
        }
        return completed == expectedCompleted;
    }

    public void addExtraTube(View view) {
        if (coins >= 50) {
            coins -= 50;
            updateUI();
            tubes.add(new Stack<>());
            adapter.notifyDataSetChanged();
            playSound(R.raw.success);
            view.setEnabled(false);
            view.setAlpha(0.5f);
        } else {
            Toast.makeText(this, "Need 50 coins!", Toast.LENGTH_SHORT).show();
        }
    }

    public void resetGameProgress(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Reset All Progress?")
                .setMessage("This will return you to Level 1 and reset coins.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
                    currentLevel = 1;
                    coins = 100;
                    undosLeft = 5;
                    loadLevel(currentLevel);
                    updateUI();
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void saveProgress() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_LEVEL, currentLevel);
        editor.apply();
    }

    private void resetAddTubeButton() {
        if (btnAddTube != null) {
            btnAddTube.setEnabled(true);
            btnAddTube.setAlpha(1.0f);
            btnAddTube.setVisibility(currentLevel < 3 ? View.GONE : View.VISIBLE);
        }
    }

    private int calculateSpanCount() {
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        return Math.max(3, (int) (screenWidthDp / 80));
    }
}