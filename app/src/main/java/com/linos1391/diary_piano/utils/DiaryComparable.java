package com.linos1391.diary_piano.utils;

import java.util.Comparator;
import java.util.Objects;

public class DiaryComparable {
    public static Comparator<? super String> TimeAscending;

    public static class TimeAscending implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            String[] a = ((String) o1).split(" ");
            String[] b = ((String) o2).split(" ");

            // Check AM and PM
            if (!Objects.equals(a[1], b[1])) {
                if (Objects.equals(a[1], "AM")) {
                    return 1;
                } else {return -1;}
            }

            // It is both AM or PM.
            return a[0].compareTo(b[0]);
        }
    }

    // TODO
}
