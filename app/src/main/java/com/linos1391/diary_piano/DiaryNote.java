package com.linos1391.diary_piano;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.Objects;

public class DiaryNote extends AppCompatActivity {

    // Variables from actions within.
    String noteTime; // Only used when `currentDiary` not null.
    int noteFulfill;
    TextView noteTitle;
    EditText noteContent;

    // Variables from get extras.
    String[] listDiary;
    String currentDiary;

    int[] listFulfill = {
            R.id.note_fulfill_1,
            R.id.note_fulfill_2,
            R.id.note_fulfill_3,
            R.id.note_fulfill_4,
            R.id.note_fulfill_5,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.diary_note);

        // give variables values:
        noteTitle = findViewById(R.id.note_title);
        noteContent = findViewById(R.id.note_content);

        // get extras from DiaryHome.java
        Intent intent = getIntent();
        listDiary = intent.getStringArrayExtra("listDiary");
        currentDiary = intent.getStringExtra("currentDiary");

        // set function to button.
        findViewById(R.id.note_exit).setOnClickListener(this::onExitNote);
        findViewById(R.id.note_delete).setOnClickListener(this::onDeleteNote);
        for (int _item : listFulfill) {
            findViewById(_item).setOnClickListener(this::onChooseFulfill);
        }

        // set previous content if it's the case.
        if (currentDiary == null) { return; }
        File file = new File(currentDiary);

        // We need all 4 TITLE (reuse when edit note) | TIME | FULFILL | CONTENT.

        // Set TITLE.
        String fileName = file.getName();
        noteTitle.setText(fileName.substring(0, fileName.length() - 5));

        // Set FULFILL and CONTENT.
        try {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            Map<?, ?> map = new Gson().fromJson(reader, Map.class);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (currentDiary != null && key.equals("time")) {
                    noteTime = (String) entry.getValue();

                } else if (key.equals("fulfill")) {
                    int fulfill_level = (int) (double) entry.getValue();
                    if (fulfill_level == 0) { continue; } // User didn't choose the level yet.
                    ImageButton chosen = findViewById(listFulfill[fulfill_level - 1]);
                    chosen.setBackgroundColor(getResources().getColor(R.color.bg_item, this.getTheme()));

                } else if (key.equals("content")) {
                    noteContent.setText((String) entry.getValue());
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void onChooseFulfill(View v) {
        // Used for fulfill buttons.
        // R.id.* cannot be used in switch case, so we use for each loop.

        int currentIndex = 0; // If only `enumerator()` existed in Java...
        for (int item : listFulfill) {
            currentIndex++;
            if (v.getId() != item) { // Change others' color to default.
                findViewById(item).setBackgroundColor(getResources().getColor(android.R.color.transparent, this.getTheme()));
                continue;
            }

            noteFulfill = currentIndex;
            v.setBackgroundColor(getResources().getColor(R.color.bg_item, this.getTheme()));
        }
    }

    public void onDeleteNote(View v) {
        // Use to delete current note.

        // Confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Confirmation Dialog")
                .setMessage("Are you sure to delete the note? There is no turning back!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    if (currentDiary != null) { // Delete file
                        File file = new File(currentDiary);

                        file.delete();
                        if (Objects.requireNonNull(Objects.requireNonNull(file.getParentFile()).listFiles()).length == 0) {
                            file.getParentFile().delete();
                        }
                    }

                    // Change to home.
                    finish();
                    startActivity(new Intent(this, Diary.class));

                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private boolean checkInList(String name, String[] diariesName) {
        for (String diaryName : diariesName) {
            if (Objects.equals(name, diaryName)) {
                // If the name is actually itself!
                if (currentDiary == null) { return true; }

                String fileName = new File(currentDiary).getName();
                if (!fileName.substring(0, fileName.length() - 5).equals(name)) { return true; }
            }
        }
        return false;
    }

    private String checkName(String name, String[] diariesName) {
        if (diariesName == null) { return name; }

        while (checkInList(name, diariesName)) {
            String[] splitName = name.split(" ");
            String endName = splitName[splitName.length - 1];

            // name in format: `...` | `... (..` | `... ..)`
            if (!endName.startsWith("(") || !endName.endsWith(")")) {
                name += " (1)";
                continue;
            }

            // name in format: `... (S)` as S not an int
            int index;
            try {
                index = Integer.parseInt(endName.substring(1, endName.length() - 1));
            } catch (NumberFormatException e) {
                name += " (1)";
                continue;
            }

            StringBuilder nameBuilder = new StringBuilder();
            int _i = -1;
            for (String currentName : splitName) {
                _i++;
                if (_i == splitName.length - 1) {
                    break;
                } // Skip last element
                nameBuilder.append(currentName).append(" ");
            }
            name = nameBuilder + "(" + (index+1) + ")";
        }
        return name;
    }

    public void onExitNote(View v) throws SecurityException {
        // Used for button that exit note.

        LocalDateTime currentTime = LocalDateTime.now(Clock.systemDefaultZone());

        // Get context from view.
        String title = noteTitle.getText().toString();
        if (title.isEmpty()) { title = "Untitled"; }

        // Preparing
        title = checkName(title, listDiary);

        File path;
        if (currentDiary != null) {
            path = new File(currentDiary).getParentFile();
        } else {
            path = new File(getFileStreamPath("diary"), currentTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).replace(" ", "_"));
            path.mkdirs();
        }

        File file = new File(path, title + ".json");
        JSONObject json = new JSONObject();

        // rename file
        if (currentDiary != null) { new File(currentDiary).renameTo(file); }

        // Write something to our json file.
        try { file.createNewFile(); } catch (IOException ignored) { }
        try {
            // With TIME, we reuse the previous time if exist.
            json.put("time", Objects.requireNonNullElseGet(noteTime, () -> currentTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))));
            // The rest are done normally.
            json.put("fulfill", noteFulfill);
            json.put("content", noteContent.getText().toString());
        } catch (JSONException e) { throw new RuntimeException(e); }
        try {
            FileWriter writerSetting = new FileWriter(file);
            writerSetting.append(json.toString(4));
            writerSetting.flush();
            writerSetting.close();
        } catch (IOException | JSONException e) { throw new RuntimeException(e); }

        // Changes to the original.
        finish();
        startActivity(new Intent(this, Diary.class));
    }

}