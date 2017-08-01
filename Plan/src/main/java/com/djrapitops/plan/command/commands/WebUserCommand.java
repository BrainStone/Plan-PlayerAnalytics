package main.java.com.djrapitops.plan.command.commands;

import com.djrapitops.plugin.command.CommandType;
import com.djrapitops.plugin.command.TreeCommand;
import com.djrapitops.plugin.settings.ColorScheme;
import main.java.com.djrapitops.plan.Permissions;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.command.commands.webuser.WebCheckCommand;
import main.java.com.djrapitops.plan.command.commands.webuser.WebDeleteCommand;
import main.java.com.djrapitops.plan.command.commands.webuser.WebLevelCommand;
import main.java.com.djrapitops.plan.command.commands.webuser.WebListUsersCommand;

/**
 * Web subcommand used to manage Web users.
 *
 * @author Rsl1122
 * @since 3.5.2
 */
public class WebUserCommand extends TreeCommand<Plan> {

    public WebUserCommand(Plan plugin, RegisterCommand register) {
        super(plugin, "webuser, web", CommandType.CONSOLE, Permissions.MANAGE_WEB.getPerm(), "Manage Webusers", "plan web");
        commands.add(register);
        setHelp(plugin);
    }

    private void setHelp(Plan plugin) {
        ColorScheme colorScheme = plugin.getColorScheme();

        String mCol = colorScheme.getMainColor();
        String sCol = colorScheme.getSecondaryColor();
        String tCol = colorScheme.getTertiaryColor();

        String[] help = new String[]{
                mCol + "Web User Manage command",
                tCol + "  Used to manage web users of the plugin",
                sCol + "  Users have a permission level:",
                tCol + "   0 - Access to all pages",
                tCol + "   1 - Access to /players & all inspect pages",
                tCol + "   2 - Access to own inspect page",
                sCol + "  Alias: /plan web"
        };

        setInDepthHelp(help);
    }

    @Override
    public void addCommands() {
        commands.add(new WebLevelCommand(plugin));
        commands.add(new WebListUsersCommand(plugin));
        commands.add(new WebCheckCommand(plugin));
        commands.add(new WebDeleteCommand(plugin));
    }
}
