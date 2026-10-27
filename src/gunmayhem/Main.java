package gunmayhem;

import gunmayhem.client.GameClientFacade;
import gunmayhem.client.TestLauncher;
import gunmayhem.core.audio.SingletonDemo;
import gunmayhem.core.prototype.PrototypeDemo;
import gunmayhem.net.GameServer;
import gunmayhem.core.gamemodes.FirstToThreeKills;
import gunmayhem.core.gamemodes.GameMode;
import gunmayhem.core.gamemodes.LastManStanding;
import gunmayhem.core.gamemodes.SuddenDeath;
import gunmayhem.core.gamemodes.TimeLimit;
import java.io.IOException;

/**
 * Entry point (pure JDK — no external frameworks). All play is client/server:
 * the server owns the simulation and every player runs a client.
 * <pre>
 *   java -cp out gunmayhem.Main server [port] [mode] [stage]  authoritative server (default port 5010)
 *   java -cp out gunmayhem.Main client &lt;host&gt; [port] [nick]   join a server
 *   java -cp out gunmayhem.Main multi [n] [host] [port]        n client windows in one JVM
 *   java -cp out gunmayhem.Main                               join a server (asks host/port/nickname)
 *   java -cp out gunmayhem.Main singleton|prototype           design pattern demos
 * </pre>
 * When no nickname is given on the command line the join screen asks for one.
 * The server picks a random stage for each match unless one is named:
 * {@code classic}, {@code glacier} or {@code drift}. The mode defaults to
 * last-man-standing; the others are first-to-three-kills, time-limit and
 * sudden-death. Mode and stage may be given in either order.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException, InterruptedException {
        String mode = args.length > 0 ? args[0].toLowerCase() : "client";
        switch (mode) {
            case "singleton" -> SingletonDemo.run();
            case "prototype" -> PrototypeDemo.run();
            case "server" -> {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : GameServer.DEFAULT_PORT;
                // Mode and stage names never overlap, so they may come in either order.
                GameMode gameMode = new LastManStanding();
                String stageId = null;
                for (int i = 2; i < args.length; i++) {
                    GameMode named = gameMode(args[i]);
                    if (named != null) {
                        gameMode = named;
                    } else {
                        stageId = args[i];
                    }
                }
                new GameServer(port, gameMode, stageId).run();
            }

            // Both client modes go through the same facade; they differ only in
            // how many windows they want and where those windows go.
            case "client" -> new GameClientFacade(
                args.length > 1 ? args[1] : "localhost",
                args.length > 2 ? Integer.parseInt(args[2]) : GameServer.DEFAULT_PORT,
                args.length > 3 ? args[3] : null).start();

            case "multi" -> TestLauncher.launch(
                args.length > 1 ? Integer.parseInt(args[1]) : 2,
                args.length > 2 ? args[2] : "localhost",
                args.length > 3 ? Integer.parseInt(args[3]) : GameServer.DEFAULT_PORT);
            default -> {
                System.err.println("Usage:");
                System.err.println("  java -cp out gunmayhem.Main server [port] [mode] [classic|glacier|drift]");
                System.err.println("  java -cp out gunmayhem.Main client <host> [port] [nickname]");
                System.err.println("  java -cp out gunmayhem.Main            (asks for host/nickname)");
                System.err.println("  java -cp out gunmayhem.Main multi [n] [host] [port]  (n client windows at once)");
                System.err.println("  java -cp out gunmayhem.Main singleton  (thread-safe singleton demo)");
                System.err.println("  java -cp out gunmayhem.Main prototype  (shallow vs deep copy demo)");
            }
        }
    }

    private static GameMode gameMode(String name) {
        return switch (name.toLowerCase()) {
            case "lastmanstanding", "last-man-standing", "lms" -> new LastManStanding();
            case "firsttothreekills", "first-to-three-kills", "kills" ->
                new FirstToThreeKills();
            case "timelimit", "time-limit", "time" -> new TimeLimit();
            case "suddendeath", "sudden-death", "sudden" -> new SuddenDeath();
            default -> null; // not a mode name: treated as a stage id
        };
    }
}
