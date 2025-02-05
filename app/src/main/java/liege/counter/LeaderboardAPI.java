package liege.counter;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface LeaderboardAPI {

    // GET-Anfrage für das gesamte Leaderboard
    @GET("leaderboard")
    Call<List<LeaderboardEntry>> getLeaderboard();

    // PUT-Anfrage zum Aktualisieren eines Leaderboard-Eintrags
    @PUT("leaderboard/{playerId}")
    Call<Void> updateEntry(@Path("playerId") String playerId, @Body LeaderboardEntry entry);
}