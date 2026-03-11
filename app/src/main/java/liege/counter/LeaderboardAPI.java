package liege.counter;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface LeaderboardAPI {

    // GET leaderboard sorted by pushups descending (Supabase PostgREST)
    @GET("leaderboard?select=*&order=pushups.desc")
    Call<List<LeaderboardEntry>> getLeaderboard();

    // UPSERT — inserts or updates the row whose id matches (Supabase PostgREST)
    @POST("leaderboard")
    @Headers("Prefer: resolution=merge-duplicates")
    Call<Void> upsertEntry(@Body LeaderboardEntry entry);
}