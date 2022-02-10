import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.io.IOUtils;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;


public class ticketNotifier extends ListenerAdapter {

    private MessageChannel pingChannel = null;
    private Role role1 = null;
    private Role role2 = null;
    private static String token;

    public static void main (String [] args) throws LoginException, IOException {

        try(FileInputStream inputStream = new FileInputStream("src/main/resources/token.txt")) {
            String input = IOUtils.toString(inputStream);
            token = input;
        }

        JDABuilder bot = JDABuilder.createDefault(token);
        bot.setActivity(Activity.watching("Searching for new threads..."));
        bot.addEventListeners(new ticketNotifier());

        //Add commands to the bot
        CommandListUpdateAction commands = bot.build().updateCommands()
                .addCommands(Commands.slash("setup", "Setting up the Bot...")
                        .addOption(OptionType.CHANNEL, "channel", "Add Channel where the Bot will ping the staff when a new Thread was created", true)
                        .addOption(OptionType.ROLE, "role-1", "Add a Role that is going to be pinged.", true)
                        .addOption(OptionType.ROLE, "role-2", "Add another Role that is going to be pinged.", false));
        commands.queue();
    }

    /**
     * Logic of the Slash Command.
     * Will be automatically called when a Slash-Command is executed.
     * @param event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("setup")) {
            event.deferReply().queue();
            pingChannel = event.getOption("channel").getAsTextChannel();
            role1 = event.getOption("role-1").getAsRole();
            role2 = event.getOption("role-2").getAsRole();
            event.getHook().sendMessage("Finished setting up.").queue();
            event.getHook().sendMessage(role1.getName() + " and " + role2.getName() + " will be now pinged in #" + pingChannel.getName()).queue();
        }
    }

    /**
     * Logic, when a Message Received
     * Will be automatically called, when a Slash-Command is executed.
     * @param event
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
            if (pingChannel != null) {
                if (event.isFromThread()) {
                    pingChannel.sendMessage("New thread detected " + role1.getAsMention() + role2.getAsMention()).queue();
                }
            } else {
                System.out.println("Error, please execute /setup first and select a channel");
            }
        }
    }
}
