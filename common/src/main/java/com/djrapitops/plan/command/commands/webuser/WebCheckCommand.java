package com.djrapitops.plan.command.commands.webuser;

import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.system.database.databases.Database;
import com.djrapitops.plan.system.settings.Permissions;
import com.djrapitops.plan.system.settings.locale.Locale;
import com.djrapitops.plan.system.settings.locale.Msg;
import com.djrapitops.plugin.api.utility.log.Log;
import com.djrapitops.plugin.command.CommandNode;
import com.djrapitops.plugin.command.CommandType;
import com.djrapitops.plugin.command.ISender;
import com.djrapitops.plugin.task.AbsRunnable;
import com.djrapitops.plugin.task.RunnableFactory;
import com.djrapitops.plugin.utilities.Verify;

import java.util.Arrays;

/**
 * Subcommand for checking WebUser permission level.
 *
 * @author Rsl1122
 * @since 3.5.2
 */
public class WebCheckCommand extends CommandNode {
    private final RunnableFactory runnableFactory;

    public WebCheckCommand(PlanPlugin plugin) {
        super("check", Permissions.MANAGE_WEB.getPerm(), CommandType.PLAYER_OR_ARGS);
        setShortHelp(Locale.get(Msg.CMD_USG_WEB_CHECK).toString());
        setArguments("<username>");

        this.runnableFactory = plugin.getRunnableFactory();
    }

    @Override
    public void onCommand(ISender sender, String commandLabel, String[] args) {
        Verify.isTrue(args.length >= 1,
                () -> new IllegalArgumentException(Locale.get(Msg.CMD_FAIL_REQ_ARGS).parse(Arrays.toString(this.getArguments()))));

        Database database = Database.getActive();
        String user = args[0];

        runnableFactory.createNew("Web user " + user + " Check", new AbsRunnable() {
            @Override
            public void run() {
                try {
                    if (!database.check().doesWebUserExists(user)) {
                        sender.sendMessage("§c[Plan] User Doesn't exist.");
                        return;
                    }
                    WebUser info = database.fetch().getWebUser(user);
                    sender.sendMessage(info.getName() + ": Permission level: " + info.getPermLevel());
                } catch (Exception ex) {
                    Log.toLog(this.getClass(), ex);
                    sender.sendMessage(Locale.get(Msg.MANAGE_INFO_FAIL).parse());
                } finally {
                    this.cancel();
                }
            }
        }).runTaskAsynchronously();
    }

}
