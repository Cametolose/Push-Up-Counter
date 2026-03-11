package liege.counter;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Retrofit interface for the free JokeAPI (v2.jokeapi.dev). */
public interface JokeApiService {

    /**
     * Fetches a single German joke.
     * Categories: Programming, Misc, Pun (family-friendly subset).
     * Note: "blacklistFlags" is the exact query parameter name required by JokeAPI v2.
     */
    @GET("joke/Programming,Misc,Pun")
    Call<JokeResponse> getGermanJoke(
            @Query("lang")           String lang,
            @Query("type")           String type,
            @Query("blacklistFlags") String blacklistFlags
    );
}
