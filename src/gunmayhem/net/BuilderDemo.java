package gunmayhem.net;

import java.util.Map;

/**
 * Runs the same building steps on both concrete builders and prints the two
 * products. Run with {@code java -cp out gunmayhem.Main builder}.
 */
public final class BuilderDemo {
    private BuilderDemo() { }

    public static void run() {
        StateSnapshot.PlayerView source = new PlayerViewBuilder()
            .id(0)
            .name("Alice")
            .position(120f, 300f)
            .lives(2)
            .kills(1)
            .alive(true)
            .weapon("Shotgun")
            .shielded(true)
            .build();

        StateSnapshot.PlayerView view = StateSnapshot.describe(source, new PlayerViewBuilder());
        Map<String, Object> json = StateSnapshot.describe(source, new PlayerJsonBuilder());

        System.out.println("PlayerViewBuilder -> " + view.getClass().getSimpleName()
            + "  name=" + view.name + " lives=" + view.lives + " weapon=" + view.weapon
            + " shielded=" + view.shielded);
        System.out.println("PlayerJsonBuilder -> " + Json.write(json));
    }
}
