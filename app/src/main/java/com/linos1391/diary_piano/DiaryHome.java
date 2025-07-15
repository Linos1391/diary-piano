package com.linos1391.diary_piano;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.linos1391.diary_piano.utils.PxConverter;
import com.linos1391.diary_piano.utils.DiaryComparable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DiaryHome extends Fragment {

    HashMap<String, String[]> diariesMap;

    /**Sorting item before getting them visualized.*/
    private void sortLayout(HashMap<String, View> layoutList, ViewGroup container) {
        ArrayList<String> sorting = new ArrayList<>(layoutList.keySet());
        sorting.sort(DiaryComparable.TimeAscending); // TODO - Use other kind of sort.

        for (String timeline : sorting) {
            View layout = layoutList.get(timeline);

            assert layout != null;
            layout.setOnClickListener(this::onEditNote);
            ((ViewGroup) container.findViewById(R.id.listDiary)).addView(layout);
        }
    }

    private View prepareDiary(ViewGroup container) {
        HashMap<String, HashMap<String, View>> layoutList = new HashMap<>();

        // Set global margin.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int _margin = (int) PxConverter.dpToPx(10, requireContext());
        params.setMargins((int) PxConverter.dpToPx(15, requireContext()), _margin, _margin, _margin);

        for (String diary : diariesMap.keySet()) {
            String[] diaryItem = diariesMap.get(diary);

            @SuppressLint("InflateParams")
            View layout = getLayoutInflater().inflate(R.layout.diary_home_item, null);
            layout.setLayoutParams(params);

            // This is for TITLE:
            TextView _title = layout.findViewById(R.id.itemTitle);
            _title.setText(diary);
            Log.d("DP_Item", "Title: " + diary);

            // Normally, there are 4 item details needed: TITLE | TIME | FULFILL | CONTENT
            // However, we don't need CONTENT here.

            String time = null;
            try {
                Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Objects.requireNonNull(diaryItem)[1])));
                Map<?, ?> map = new Gson().fromJson(reader, Map.class);
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    // This is for TIME | FULFILL
                    Object key = entry.getKey();
                    if (key.equals("time")) { // TODO - date to time
                        TextView _date = layout.findViewById(R.id.itemTime);
                        time = (String) entry.getValue();
                        _date.setText(time);
                        Log.d("DP_Item", "Time: " + time);
                    } else if (key.equals("fulfill")) {
                        ImageView _fulfill = layout.findViewById(R.id.itemFulfill);
                        int fulfill_level = (int) (double) entry.getValue();
                        _fulfill.setImageResource(
                                (int) Objects.requireNonNull(R.drawable.class.getField("home_fulfill_" + fulfill_level).get(R.drawable.class))
                        );
                        Log.d("DP_Item", "Fulfill: " + fulfill_level);
                    }

                }
            } catch (FileNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            HashMap<String, View> timeMap = new HashMap<>();
            if (layoutList.containsKey(diaryItem[0])) {
                timeMap = layoutList.get(diaryItem[0]);
                layoutList.remove(diaryItem[0]);
            }
            Objects.requireNonNull(timeMap).put(time, layout);
            layoutList.put(diaryItem[0], timeMap);
        }

        // Add all what we have.

        for (String date : layoutList.keySet()) {
            // Show the date
            TextView textDate = new TextView(getContext());
            textDate.setText(date);
            textDate.setTextSize(30);
            textDate.setTextColor(getResources().getColor(R.color.text, requireActivity().getTheme()));
            textDate.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            textDate.setLayoutParams(params);
            ((ViewGroup) container.findViewById(R.id.listDiary)).addView(textDate);

            // Add item.
            sortLayout(Objects.requireNonNull(layoutList.get(date)), container);
        }
        return container;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        assert container != null;

        // Prepare items
        Diary diaryActivity = (Diary) getActivity();
        assert diaryActivity != null;
        diariesMap = diaryActivity.getDiary();

        View homeLayout;
        if (diariesMap == null) {
            // There is no item, so we set it empty.
            homeLayout = inflater.inflate(R.layout.diary_home_empty, container, false);

            // Attach function to button.
            View createNoteEmpty = homeLayout.findViewById(R.id.createNoteEmpty);
            createNoteEmpty.setOnClickListener(this::onCreateNote);
        } else {

            // There are items, so let it prepares.
            ViewGroup diariesContainer = (ViewGroup) inflater.inflate(R.layout.diary_home, container, false);
            homeLayout = prepareDiary(diariesContainer);

            // Attach function to button.
            View createNote = homeLayout.findViewById(R.id.createNote);
            createNote.setOnClickListener(this::onCreateNote);
        }
        return homeLayout;
    }

    /**This function changes into new activity.*/
    public void onCreateNote(View v) {
        Intent diaryNote = new Intent(getContext(), DiaryNote.class);
        if (diariesMap != null) {
            diaryNote.putExtra("listDiary", diariesMap.keySet().toArray(new String[0]));
        }

        startActivity(diaryNote);
        requireActivity().finish();
    }

    /**This function also changes into new activity.*/
    public void onEditNote(View v) {
        Intent diaryNote = new Intent(getContext(), DiaryNote.class);

        // This one kinda complicated, just know that it tries to get `itemTitle`
        String currentTitle = (String) ((TextView) ((ViewGroup) ((ViewGroup) v).getChildAt(1)).getChildAt(0)).getText();

        diaryNote.putExtra("listDiary", diariesMap.keySet().toArray(new String[0]));
        diaryNote.putExtra("currentDiary", Objects.requireNonNull(diariesMap.get(currentTitle))[1]);

        startActivity(diaryNote);
        requireActivity().finish();
    }
}