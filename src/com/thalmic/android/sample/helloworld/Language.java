package com.thalmic.android.sample.helloworld;

import java.io.Console;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;

public class Language {
    public static String[] getResponses(String s) {
        String result = process(s.toLowerCase());
        Pattern ore = Pattern.compile(" or");
        Matcher matcher = ore.matcher(result);
        if (matcher.find()) {
            result = result.substring(0, matcher.start()) + result.substring(matcher.end(), result.length());
        }
        return result.split(", ");
    }

    private static String process(String message) {
        Pattern comma_separated = Pattern.compile("([a-zA-Z]+, )+or [a-zA-Z]+");
        Matcher matcher = comma_separated.matcher(message);

        if (matcher.find()) {
            return matcher.group();
        }

        Pattern two_sep_or = Pattern.compile("([a-zA-Z]+) or ([a-zA-Z]+)");
        matcher = two_sep_or.matcher(message);

        if (matcher.find()) {
            return matcher.group(1) + ", " + matcher.group(2);
        }

        Pattern when = Pattern.compile("when");
        matcher = when.matcher(message);

        if (matcher.find()) {
            return "5 minutes, 10 minutes, 30 minutes, or 1 hour";
        }

        Pattern what_time = Pattern.compile("what time");
        matcher = what_time.matcher(message);

        if (matcher.find()) {
            return "5 minutes, 10 minutes, 30 minutes, or 1 hour";
        }

        Pattern how_long = Pattern.compile("how long");
        matcher = how_long.matcher(message);

        if (matcher.find()) {
            return "5 minutes, 10 minutes, 30 minutes, or 1 hour";
        }

        Pattern do_you = Pattern.compile("do you");
        matcher = do_you.matcher(message);

        if (matcher.find()) {
            return "yes, no, or not sure";
        }

        Pattern are_you = Pattern.compile("are you");
        matcher = are_you.matcher(message);

        if (matcher.find()) {
            return "yes, no, or not sure";
        }

        return "none";

    /*
    boolean found = false;
    while (matcher.find()) {
      console.format("I found the text" +
          " \"%s\" starting at " +
          "index %d and ending at index %d.%n",
          matcher.group(),
          matcher.start(),
          matcher.end());
      found = true;
    }
    if(!found){
      console.format("No match found.%n");
    }*/
    }
}