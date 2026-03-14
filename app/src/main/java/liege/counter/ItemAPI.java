package liege.counter;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for the player_traps Supabase table.
 */
public interface ItemAPI {

    /** Get traps received by a specific player that are still active. */
    @GET("player_traps?select=*&active=eq.true&order=created_at.desc")
    Call<List<PlayerTrap>> getActiveTrapsForReceiver(@Query("receiver_id") String receiverFilter);

    /** Get traps sent by a specific player today (for checking daily limit). */
    @GET("player_traps?select=*&order=created_at.desc")
    Call<List<PlayerTrap>> getTrapsForDate(
            @Query("receiver_id") String receiverFilter,
            @Query("created_at") String dateFilter);

    /** Send a new trap to a player. */
    @POST("player_traps")
    @Headers("Prefer: return=representation")
    Call<List<PlayerTrap>> sendTrap(@Body PlayerTrap trap);

    /** Update a trap (e.g. mark as negated or inactive). */
    @PATCH("player_traps")
    Call<Void> updateTrap(
            @Query("id") String idFilter,
            @Body PlayerTrap updates);
}
