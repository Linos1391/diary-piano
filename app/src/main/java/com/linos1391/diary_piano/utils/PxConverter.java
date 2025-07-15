package com.linos1391.diary_piano.utils;

import android.content.Context;
import android.util.TypedValue;

public class PxConverter {
    public static int dpToPx(float dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
