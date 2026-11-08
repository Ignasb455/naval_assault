package gunmayhem.net.commands;

import java.util.List;

/**
 * Several admin commands treated as one. The group is an {@link AdminCommand}
 * itself, so {@link CommandHistory} stores it as a single entry and one
 * {@code undo} takes the whole batch back — and a group may hold other groups,
 * which is how a scenario is built out of smaller named ones.
 *
 * <p>Children are undone newest first. A later step may depend on an earlier
 * one, so unwinding in the order they ran would put things back wrong.
 */
public final class CommandGroup implements AdminCommand {
    private final String label;
    private final List<AdminCommand> children;

    public CommandGroup(String label, List<AdminCommand> children) {
        this.label = label;
        this.children = List.copyOf(children);
    }

    @Override
    public void execute() {
        for (AdminCommand child : children) {
            child.execute();
        }
    }

    /** @return false if any child could no longer be reverted. */
    @Override
    public boolean undo() {
        boolean reverted = true;
        for (int i = children.size() - 1; i >= 0; i--) {
            reverted &= children.get(i).undo();
        }
        return reverted;
    }

    @Override
    public String describe() {
        return label + " (" + children.size() + (children.size() == 1 ? " step)" : " steps)");
    }

    public List<AdminCommand> children() {
        return children;
    }

    /** The group and everything under it, indented one level per depth. */
    public String tree() {
        StringBuilder sb = new StringBuilder();
        append(sb, this, 0);
        return sb.toString();
    }

    private static void append(StringBuilder sb, AdminCommand command, int depth) {
        sb.append("  ".repeat(depth)).append(command.describe()).append(System.lineSeparator());
        if (command instanceof CommandGroup group) {
            for (AdminCommand child : group.children) {
                append(sb, child, depth + 1);
            }
        }
    }
}
