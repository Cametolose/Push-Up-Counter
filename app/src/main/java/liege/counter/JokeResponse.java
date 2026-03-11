package liege.counter;

/** Response model for a single-type joke from v2.jokeapi.dev. */
public class JokeResponse {
    private boolean error;
    private String  type;
    private String  joke;
    private String  setup;
    private String  delivery;

    public boolean isError()     { return error; }
    public String  getType()     { return type; }
    public String  getJoke()     { return joke; }
    public String  getSetup()    { return setup; }
    public String  getDelivery() { return delivery; }
}
