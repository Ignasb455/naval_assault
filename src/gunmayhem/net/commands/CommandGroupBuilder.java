package gunmayhem.net.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the commands typed between {@code group <label>} and
 * {@code group end}. One of these per open level, so a group started while
 * another is still recording simply becomes a child of it.
 */
public final class CommandGroupBuilder {
    private final String label;
    private final List<AdminCommand> children = new ArrayList<>();

    public CommandGroupBuilder(String label) {
        this.label = label;
    }

    public void add(AdminCommand command) {
        children.add(command);
    }

    public String label() {
        return label;
    }

    public int size() {
        return children.size();
    }

    public CommandGroup build() {
        return new CommandGroup(label, children);
    }
}
