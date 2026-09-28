package com.mersad.gold18;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class PriceHistory {

    private static final String PREFS = "gold_history";

    private static final long ONE_MINUTE = 60_000L;
    private static final long FIFTEEN_MINUTES = 15 * ONE_MINUTE;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;

    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long SEVEN_DAYS = 7 * ONE_DAY;
    private static final long THIRTY_DAYS = 30 * ONE_DAY;

    private static String getKey(String source) {
        if ("milli".equals(source)) {
            return "prices_milli";
        }
        return "prices_digikala";
    }

    public static synchronized void add(
            Context context,
            long price,
            String source) {

        try {
            SharedPreferences prefs =
                    context.getSharedPreferences(
                            PREFS,
                            Context.MODE_PRIVATE
                    );

            String key = getKey(source);

            String oldData =
                    prefs.getString(key, "[]");

            JSONArray array =
                    new JSONArray(oldData);

            long now =
                    System.currentTimeMillis();

            if (array.length() == 0) {

                addItem(array, price, now);

                saveAndClean(
                        prefs,
                        key,
                        array,
                        now
                );

                return;
            }

            JSONObject lastItem =
                    array.getJSONObject(
                            array.length() - 1
                    );

            long lastTime =
                    lastItem.getLong("time");

            long age =
                    now - lastTime;

            long requiredInterval;

            if (age < ONE_DAY) {
                requiredInterval = ONE_MINUTE;
            } else if (age < SEVEN_DAYS) {
                requiredInterval = FIFTEEN_MINUTES;
            } else {
                requiredInterval = ONE_HOUR;
            }

            if (age >= requiredInterval) {
                addItem(array, price, now);
            }

            saveAndClean(
                    prefs,
                    key,
                    array,
                    now
            );

        } catch (Exception ignored) {
        }
    }

    private static void addItem(
            JSONArray array,
            long price,
            long time
    ) throws Exception {

        JSONObject item =
                new JSONObject();

        item.put("price", price);
        item.put("time", time);

        array.put(item);
    }

    private static void saveAndClean(
            SharedPreferences prefs,
            String key,
            JSONArray array,
            long now
    ) throws Exception {

        JSONArray cleaned =
                new JSONArray();

        long cutoff =
                now - THIRTY_DAYS;

        for (int i = 0;
             i < array.length();
             i++) {

            JSONObject item =
                    array.getJSONObject(i);

            long time =
                    item.getLong("time");

            if (time >= cutoff) {
                cleaned.put(item);
            }
        }

        while (cleaned.length() > 4000) {

            JSONArray smaller =
                    new JSONArray();

            for (int i = 1;
                 i < cleaned.length();
                 i++) {

                smaller.put(
                        cleaned.get(i)
                );
            }

            cleaned = smaller;
        }

        prefs.edit()
                .putString(
                        key,
                        cleaned.toString()
                )
                .apply();
    }

    public static synchronized JSONArray get(
            Context context,
            String source) {

        try {

            SharedPreferences prefs =
                    context.getSharedPreferences(
                            PREFS,
                            Context.MODE_PRIVATE
                    );

            return new JSONArray(
                    prefs.getString(
                            getKey(source),
                            "[]"
                    )
            );

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    public static synchronized void add(
            Context context,
            long price) {

        add(
                context,
                price,
                "digikala"
        );
    }

    public static synchronized JSONArray get(
            Context context) {

        return get(
                context,
                "digikala"
        );
    }
}
