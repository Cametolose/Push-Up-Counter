package liege.counter;

import android.util.Log;

/**
 * Supabase configuration — loaded from BuildConfig, which reads values from gradle.properties
 * (or local.properties for development). Do NOT commit real credentials to source control.
 *
 * Quick-start:
 * 1. Create a free project at https://supabase.com
 * 2. Go to Project Settings → API and note:
 *      • "Project URL"          → your SUPABASE_URL  (append  /rest/v1/)
 *      • "anon / public" key    → your SUPABASE_ANON_KEY
 * 3. Add the following lines to your  gradle.properties  (or  local.properties):
 *        SUPABASE_URL=https://abcdefghij.supabase.co/rest/v1/
 *        SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 4. In the Supabase Table Editor create a table named  leaderboard  with RLS disabled
 *    (or appropriate policies) and the following columns:
 *
 *      id                   text   PRIMARY KEY
 *      name                 text
 *      pushups              int8
 *      level                int8
 *      currentXp            int8
 *      totalXp              int8
 *      xpForNextLevel       int8
 *      totalQuestsCompleted int8
 *      quest1Completions    int8
 *      quest2Completions    int8
 *      quest3Completions    int8
 *      bonusXpCollected     int8
 *      dailyPushups         int8
 *      weeklyPushups        int8
 *      monthlyPushups       int8
 *      yearlyPushups        int8
 *      avgPushupsPerDay     float8
 *      avgPushupsPerWeek    float8
 *      avgPushupsPerMonth   float8
 *
 *    Note: column names must match the Java field names above (camelCase)
 *    because Gson serialises without @SerializedName annotations.
 *    In the Supabase SQL editor use quoted identifiers, e.g.:
 *      ALTER TABLE leaderboard ADD COLUMN "currentXp" int8;
 */
public class SupabaseConfig {

    /** Full REST base URL — must end with "/rest/v1/". */
    public static final String SUPABASE_URL      = BuildConfig.SUPABASE_URL;

    /** The "anon / public" key from Project Settings → API. */
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static final String TAG = "SupabaseConfig";

    /** Logs a warning when the project has not yet been configured. */
    public static void validateConfig() {
        if (SUPABASE_URL.contains("YOUR_PROJECT_REF")
                || SUPABASE_ANON_KEY.equals("YOUR_ANON_KEY_HERE")) {
            Log.w(TAG, "Supabase credentials are placeholders — leaderboard will not work. "
                    + "See SupabaseConfig.java for setup instructions.");
        }
    }
}
