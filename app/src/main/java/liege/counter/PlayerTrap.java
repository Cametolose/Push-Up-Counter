package liege.counter;

/**
 * Data class for traps exchanged between players via Supabase.
 * Maps to the 'player_traps' table.
 */
public class PlayerTrap {
    private String id;           // UUID primary key
    private String sender_id;    // sender's player id
    private String sender_name;  // sender's display name
    private String receiver_id;  // receiver's player id
    private String receiver_name;// receiver's display name
    private String trap_type;    // e.g. "half_xp", "minus_50", "minus_100"
    private String created_at;   // ISO timestamp
    private String expires_at;   // ISO timestamp (for time-based traps)
    private boolean active;      // whether the trap is still active
    private boolean negated;     // whether it was negated by a Negate Trap item

    public PlayerTrap() {}

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSender_id() { return sender_id; }
    public void setSender_id(String sender_id) { this.sender_id = sender_id; }

    public String getSender_name() { return sender_name; }
    public void setSender_name(String sender_name) { this.sender_name = sender_name; }

    public String getReceiver_id() { return receiver_id; }
    public void setReceiver_id(String receiver_id) { this.receiver_id = receiver_id; }

    public String getReceiver_name() { return receiver_name; }
    public void setReceiver_name(String receiver_name) { this.receiver_name = receiver_name; }

    public String getTrap_type() { return trap_type; }
    public void setTrap_type(String trap_type) { this.trap_type = trap_type; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getExpires_at() { return expires_at; }
    public void setExpires_at(String expires_at) { this.expires_at = expires_at; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isNegated() { return negated; }
    public void setNegated(boolean negated) { this.negated = negated; }
}
