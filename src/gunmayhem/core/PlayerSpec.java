package gunmayhem.core;

/**
 * Who is taking part in a match: the server's slot id (which also picks the
 * player's colour) plus the nickname that player chose when joining.
 */
public record PlayerSpec(int id, String name) { }
