package com.linos1391.diary_piano;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.PorterDuff;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.linos1391.diary_piano.utils.PianoTranslate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Piano extends AppCompatActivity {

    // For resizing
    private boolean sizeDouble = false;

    // For editing password.
    private boolean isTyping = false;
    private final ArrayList<String> newPassword = new ArrayList<>();

    // To recognise password.
    private final PianoTranslate translator = new PianoTranslate();
    private String[] passwordList = {"C#1", "C#1", "C1", "C1", "B0", "D1", "B0", "D1", "B0", "A0"}; // Default pass.
    private int passwordNext = 0;

    // For piano's sounding.
    private final HashMap<String, Integer> pianoAudio = new HashMap<>();
    private final SoundPool audioPool = new SoundPool.Builder().setMaxStreams(88).build();

    private void addAudio() {
        for (int i = 1; i <= 88; i++) {
            try (AssetFileDescriptor f = getAssets().openFd("piano_sound/" + i + ".mp3")) {
                audioPool.load(f, 1);
            } catch (IOException e) { throw new RuntimeException(e); }
        }

    }

    private void resizeNotes(ViewGroup mainView, int width, int height) {
        ViewGroup container = mainView.findViewById(R.id.piano_holder);
        // Prepare consts.
        final int blackHeight = height * 5/8;
        final int whiteWidth = width / 9;
        final int blackWidth = width / 12;

        for (int g = 0; g < 9; g++) { // Scan for 9 piano groups
            ConstraintLayout group = (ConstraintLayout) container.getChildAt(g);

            switch (g) {
                case 0: // First special group.
                    for (int i = 0; i < 3; i++) {
                        View note = group.getChildAt(i);
                        ViewGroup.LayoutParams layoutParams = note.getLayoutParams();
                        if (i == 2) {
                            layoutParams.height = blackHeight;
                            layoutParams.width = blackWidth;
                        } else {
                            layoutParams.height = height;
                            layoutParams.width = whiteWidth;
                        }
                        note.setLayoutParams(layoutParams);
                    }
                    break;
                case 8: // Last special group.
                    ViewGroup.LayoutParams layout = group.getChildAt(0).getLayoutParams();
                    layout.height = height;
                    layout.width = whiteWidth;
                    group.getChildAt(0).setLayoutParams(layout);
                    break;
                default: // The rest.
                    for (int i = 0; i < 12; i++) {
                        View note = group.getChildAt(i);
                        ViewGroup.LayoutParams layoutParams = note.getLayoutParams();
                        if (i < 7) {
                            layoutParams.height = height;
                            layoutParams.width = whiteWidth;
                        } else {
                            layoutParams.height = blackHeight;
                            layoutParams.width = blackWidth;
                        }
                        note.setLayoutParams(layoutParams);
                    }
                    break;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setScrollView(ViewGroup mainView, int width) {
        HorizontalScrollView scrollView = mainView.findViewById(R.id.piano_scroll);
        scrollView.setOnTouchListener((v, event) -> true);

        View scrollLeft = mainView.findViewById(R.id.piano_scroll_left);
        scrollLeft.setOnClickListener(v -> scrollView.scrollBy(-width, 0));

        View scrollRight = mainView.findViewById(R.id.piano_scroll_right);
        scrollRight.setOnClickListener(v -> scrollView.scrollBy(width, 0));
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    private void createPiano(ViewGroup mainView) {
        ViewGroup container = mainView.findViewById(R.id.piano_holder);

        // Create the special left group included A0, A#0, B0.
        ViewGroup specialLeftGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.piano_keyboard0, null);
        specialLeftGroup.setId(View.generateViewId());
        for (int i = 0; i < 3; i++) {
            View current = specialLeftGroup.getChildAt(i);
            current.setOnTouchListener(this::onPressNote);
            pianoAudio.put(specialLeftGroup.getId() + "_" + current.getId(), translator.getEntry(0, i));
        }
        container.addView(specialLeftGroup);

        // Create all 7 normal groups.
        for (int group = 1; group <= 7; group++) {
            ViewGroup pianoGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.piano_keyboard, null);
            pianoGroup.setId(View.generateViewId());

            TextView pianoCurrent = pianoGroup.findViewById(R.id.current_group);
            pianoCurrent.setText("C" + group);

            for (int i = 0; i < 12; i++) {
                View current = pianoGroup.getChildAt(i);
                current.setOnTouchListener(this::onPressNote);
                pianoAudio.put(pianoGroup.getId() + "_" + current.getId(), translator.getEntry(group, i));
            }
            container.addView(pianoGroup);
        }

        // Create the last C8.
        ViewGroup specialRightGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.piano_keyboard8, null);
        specialRightGroup.setId(View.generateViewId());

        View current = specialRightGroup.getChildAt(0);
        current.setOnTouchListener(this::onPressNote);
        pianoAudio.put(specialRightGroup.getId() + "_" + current.getId(), 88); // C8

        container.addView(specialRightGroup);

        // Resize notes for relative UI.
        HorizontalScrollView scrollview = (HorizontalScrollView) container.getParent();
        final ViewTreeObserver vto = scrollview.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int width = scrollview.getMeasuredWidth();
                    resizeNotes(mainView, width, scrollview.getMeasuredHeight());
                    setScrollView(mainView, width/9);
                    if (vto.isAlive()) { // Double check.
                        vto.removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }

    @SuppressLint("InflateParams")
    private void preparePiano(ViewGroup mainView) {
        ViewGroup board = (ViewGroup) getLayoutInflater().inflate(R.layout.piano_full_board, null);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.weight = 1;
        board.setLayoutParams(params);

        createPiano(board);
        mainView.addView(board);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addAudio();

        // Main content.
        ViewGroup mainView = (ViewGroup) getLayoutInflater().inflate(R.layout.piano, null);
        preparePiano(mainView.findViewById(R.id.piano_container));

        // Add title.
        ConstraintLayout layout = mainView.findViewById(R.id.piano_layout);
        View title = getLayoutInflater().inflate(R.layout.piano_title, null);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        title.setLayoutParams(params);
        layout.addView(title);

        // Set content.
        setContentView(mainView);

        // NOTICE: I mean to place password here in case user forgot.
        File file = getFileStreamPath("pass.json");

        if (!file.exists()) { // If the password is not created, we recreated.
            JSONObject json = new JSONObject();

            try { file.createNewFile(); } catch (IOException ignored) { }
            try {
                json.put("password", "C#1-C#1-C1-C1-B0-D1-B0-D1-B0-A0");
            } catch (JSONException e) { throw new RuntimeException(e); }
            try {
                FileWriter writer = new FileWriter(file);
                writer.append(json.toString(4));
                writer.flush();
                writer.close();
            } catch (IOException | JSONException e) { throw new RuntimeException(e); }
        } else { // Get the password if there is one.
            try {
                Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                Map<?, ?> map = new Gson().fromJson(reader, Map.class);
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    if (key.equals("password")) { passwordList = ((String) entry.getValue()).split("-"); }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
     }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @SuppressLint("ResourceAsColor, InflateParams")
    public boolean onPressNote(View v, MotionEvent e) {
        Integer soundID = pianoAudio.get(((View) v.getParent()).getId() + "_" + v.getId());
        if (soundID == null) { return false;}

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                audioPool.play(soundID, 1, 1, 0, 0, 1);
                v.getBackground().setColorFilter(R.color.note_pressed, PorterDuff.Mode.DST_ATOP);
                v.invalidate();

                String pressedNoted = translator.getName(soundID);

                // Activated when editing password.
                if (isTyping) {
                    newPassword.add(pressedNoted);
                    TextView edit = findViewById(R.id.piano_edit);
                    int size = newPassword.size();
                    if (size > 10) { // We gotta limit into 10 last digits.
                        edit.setText(String.join("-", newPassword.subList(size-11, size-1)));
                    } else {
                        edit.setText(String.join("-", newPassword));
                    }
                    return true;
                }

                // Check on password.
                if (Objects.equals(pressedNoted, passwordList[passwordNext])) {
                    passwordNext++;
                    if (passwordList.length == passwordNext) {
                        new AlertDialog.Builder(this)
                                .setTitle("Welcome Back!")
                                .setMessage("Please choose the options below.")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton("To Diary", (dialog, whichButton) -> {
                                    startActivity(new Intent(this, Diary.class));
                                    passwordNext = 0;
                                })
                                .setNeutralButton("Edit Password", (dialog, whichButton) -> {
                                    isTyping = true;
                                    passwordNext = 0;

                                    ConstraintLayout layout = findViewById(R.id.piano_layout);
                                    layout.removeViewAt(0);
                                    View title = getLayoutInflater().inflate(R.layout.piano_title_edit, null);
                                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
                                    title.setLayoutParams(params);
                                    layout.addView(title);
                                })
                                .setNegativeButton(android.R.string.cancel, null).show();
                    }
                } else { passwordNext = 0; }

                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.getBackground().clearColorFilter();
                v.invalidate();
                return true;
        }
        return false;
    }

    public void onButtonDel(View v) {
        if (newPassword.isEmpty()) { return; }
        newPassword.remove(newPassword.size() - 1); // `removeLast()` at home

        TextView title = findViewById(R.id.piano_edit);
        title.setText(String.join("-", newPassword));
    }

    @SuppressLint("InflateParams")
    public void onButtonDone(View v) {
        if (newPassword.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning Dialog")
                    .setMessage("You must type in a password.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(android.R.string.cancel, null).show();
            return;
        }

        String pass = String.join("-", newPassword);

        new AlertDialog.Builder(this)
                .setTitle("Confirmation Dialog")
                .setMessage("Are you sure to change the password to `"+pass+"` ? Recheck before confirmation!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    isTyping = false;

                    passwordList = newPassword.toArray(new String[0]);
                    newPassword.clear();

                    File file = getFileStreamPath("pass.json");
                    if (!file.exists()) {
                        try { file.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
                    }

                    JSONObject json = new JSONObject();
                    try {
                        json.put("password", pass);
                    } catch (JSONException e) { throw new RuntimeException(e); }
                    try {
                        FileWriter writer = new FileWriter(file);
                        writer.append(json.toString(4));
                        writer.flush();
                        writer.close();
                    } catch (IOException | JSONException e) { throw new RuntimeException(e); }

                    ConstraintLayout layout = findViewById(R.id.piano_layout);
                    layout.removeViewAt(0);
                    View title = getLayoutInflater().inflate(R.layout.piano_title, null);
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
                    title.setLayoutParams(params);
                    layout.addView(title);
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @SuppressLint("InflateParams")
    public void onButtonExit(View v) {
        isTyping = false;
        newPassword.clear();

        ConstraintLayout layout = findViewById(R.id.piano_layout);
        layout.removeViewAt(0);
        View title = getLayoutInflater().inflate(R.layout.piano_title, null);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        title.setLayoutParams(params);
        layout.addView(title);
    }

    public void onTutorial(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Linos1391/diary-piano"));
        startActivity(browserIntent);
    }

    public void onDouble(View v) {
        sizeDouble = !sizeDouble;

        LinearLayout container = findViewById(R.id.piano_container);
        container.removeAllViews();

        if (sizeDouble) {
            ((Button) v).setText("×2");
            preparePiano(findViewById(R.id.piano_container));
            preparePiano(findViewById(R.id.piano_container));
        }
        else {
            ((Button) v).setText("×1");
            preparePiano(findViewById(R.id.piano_container));
        }
    }
}
