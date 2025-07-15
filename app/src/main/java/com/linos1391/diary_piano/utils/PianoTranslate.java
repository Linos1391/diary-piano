package com.linos1391.diary_piano.utils;


public class PianoTranslate {
    private final String[] noteName0 = {"A0", "A#0", "B0"};
    private final String[] noteName = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public String getName(int soundID) {
        if (soundID == 88) {
            return "C8";
        } else if (soundID <= 3) {
            return noteName0[soundID - 1];
        } else {
            soundID -= 4; // excluded first group and because soundID begin with 1.
            return noteName[soundID%12] + (soundID/12 + 1);
        }
    }

    private final int[] noteSound0 = {1, 3, 2};
    private final int[] noteSound = {1, 3, 5, 6, 8, 10, 12, 2, 4, 7, 9, 11};

    public int getEntry(int group, int note) {
        switch (group) {
            case 0:
                return noteSound0[note];
            case 8:
                return 88;
            default:
                return noteSound[note] + 12 * (group - 1) + 3;
        }
    }
}
