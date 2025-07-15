package com.linos1391.diary_piano;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class Diary extends AppCompatActivity {

    public HashMap<String, String[]> getDiary() {
        // Key as name.
        // Value as setting file.

        File root = getFileStreamPath("diary");

        // Check just in case
        if (root.mkdir()) {
            Log.d("DP_File", "Diary were not existed");
        } else {
            Log.d("DP_File", "Diaries were existed");
        }

        // Declare variables.
        HashMap<String, String[]> diariesMap = new HashMap<>(); //TODO

        // Search for diaries.
        File[] dirs = root.listFiles();
        if (dirs == null || dirs.length == 0) {return null;}

        // Then we only take file that has all TITLE | TIME | FULFILL | CONTENT
        // TITLE is file's name without `.json`

        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {return null;}

            for (File file : files) {
                boolean existDate = false;
                boolean existFulfill = false;
                boolean existContext = false;

                try {
                    Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    Map<?, ?> map = new Gson().fromJson(reader, Map.class);
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        Object key = entry.getKey();
                        if (key.equals("time")) { existDate = true; }
                        else if (key.equals("fulfill")) { existFulfill = true; }
                        else if (key.equals("content")) { existContext = true; }
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                if (existDate && existFulfill && existContext) {
                    String fileName = file.getName();
                    fileName = fileName.substring(0, fileName.length() - 5);
                    diariesMap.put(fileName, new String[]{dir.getName().replace("_", " "), file.getPath()});
                    Log.d("DP_File", "Got diary: " + fileName);
                }
            }
        }
        return diariesMap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.diary);
    }

    public void onExit(View v) {
        finish();
    }
}