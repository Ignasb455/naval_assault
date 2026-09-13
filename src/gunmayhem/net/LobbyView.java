package gunmayhem.net;

import gunmayhem.core.GameConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What every client sees while the match is still filling up: who has joined so
 * far and how long is left on the shared start countdown. The server owns the
 * clock and broadcasts this each tick, so all clients show the same number.
 */
public final class LobbyView {

    public static final class Seat {
        public final int id;
        public final String name;

        public Seat(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public final List<Seat> seats;
    public final boolean counting;      // true once MIN_PLAYERS are connected
    public final float secondsLeft;     // remaining countdown, 0 when not counting
    public final int minPlayers;
    public final int maxPlayers;

    public LobbyView(List<Seat> seats, boolean counting, float secondsLeft,
                     int minPlayers, int maxPlayers) {
        this.seats = seats;
        this.counting = counting;
        this.secondsLeft = secondsLeft;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    /** Whole seconds shown on the countdown (rounds up, so "15" is visible at the start). */
    public int displaySeconds() {
        return (int) Math.ceil(Math.max(0f, secondsLeft));
    }

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "lobby");
        root.put("counting", counting);
        root.put("secondsLeft", secondsLeft);
        root.put("minPlayers", minPlayers);
        root.put("maxPlayers", maxPlayers);

        List<Object> list = new ArrayList<>();
        for (Seat s : seats) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id);
            m.put("name", s.name);
            list.add(m);
        }
        root.put("seats", list);
        return Json.write(root);
    }

    @SuppressWarnings("unchecked")
    public static LobbyView fromJson(String json) {
        Map<String, Object> root = Json.parseObject(json);
        List<Seat> seats = new ArrayList<>();
        Object rawSeats = root.get("seats");
        if (rawSeats instanceof List<?> l) {
            for (Object o : l) {
                Map<String, Object> m = (Map<String, Object>) o;
                int id = m.get("id") instanceof Number n ? n.intValue() : 0;
                Object name = m.get("name");
                seats.add(new Seat(id, name instanceof String s ? s : ""));
            }
        }
        return new LobbyView(
            seats,
            Boolean.TRUE.equals(root.get("counting")),
            root.get("secondsLeft") instanceof Number n ? n.floatValue() : 0f,
            root.get("minPlayers") instanceof Number n ? n.intValue() : GameConfig.MIN_PLAYERS,
            root.get("maxPlayers") instanceof Number n ? n.intValue() : GameConfig.MAX_PLAYERS);
    }
}
