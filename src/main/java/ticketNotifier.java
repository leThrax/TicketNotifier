import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.io.IOUtils;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.Scanner;


public class ticketNotifier extends ListenerAdapter {

    private static MessageChannel pingChannel = null;
    private static Role role1 = null;
    private static Role role2 = null;
    private static String token;

    public static void main(String[] args) throws LoginException, IOException {

        try (FileInputStream inputStream = new FileInputStream("src/main/resources/token.txt")) {
            String input = IOUtils.toString(inputStream);
            token = input;
        }

        JDABuilder bot = JDABuilder.createDefault(token);
        bot.setActivity(Activity.watching("Searching for new threads..."));
        bot.addEventListeners(new ticketNotifier());

        File settingsFile = new File("src/main/resources/openThreads");
        if (settingsFile.createNewFile()) {
            System.out.println("Settings file didn't exist, created new one");
        }
        else {
            System.out.println("Settings file exists");
        }
        if (settingsFile.length() != 0) {
            Scanner reader = new Scanner(settingsFile);
            String[] setting = reader.nextLine().split("-");
            if (setting.length == 3) {
                pingChannel = bot.build().getTextChannelById(setting[0]);
                role1 = bot.build().getRoleById(setting[1]);
                role2 = bot.build().getRoleById(setting[2]);
            }
        }

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
     *
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
            if (role2 != null) {
                event.getHook().sendMessage(role1.getName() + " and " + role2.getName() + " will be now pinged in #" + pingChannel.getName()).queue();
            } else {
                event.getHook().sendMessage(role1.getName() + " will be now pinged in #" + pingChannel.getName()).queue();
            }
            try {
                addSettingsToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logic, when a Message Received
     * Will be automatically called, when a Slash-Command is executed.
     *
     * @param event
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        boolean threadExists = false;
        if (event.getMessage().getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
            if (pingChannel != null) {
                if (event.isFromThread()) {
                    File openThreads = new File("src/main/resources/openThreads");
                    if (openThreads.length() == 0) {
                        try {
                            FileWriter writer = new FileWriter("src/main/resources/openThreads", true);
                            writer.write(Long.toString(event.getThreadChannel().getIdLong()) + "\n");
                            pingChannel.sendMessage("New thread detected " + role1.getAsMention() + role2.getAsMention()).queue();
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Scanner reader = new Scanner(openThreads);
                            while (reader.hasNextLine()) {
                                long threadID = Long.parseLong(reader.nextLine());
                                if (threadID == event.getThreadChannel().getIdLong()) {
                                    threadExists = true;
                                    break;
                                }
                            }
                            reader.close();
                            if (!threadExists) {
                                try {
                                    FileWriter writer = new FileWriter("src/main/resources/openThreads", true);
                                    writer.write(Long.toString(event.getThreadChannel().getIdLong()) + "\n");
                                    pingChannel.sendMessage("New thread detected " + role1.getAsMention() + role2.getAsMention()).queue();
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                System.out.println("Error, please execute /setup first and select a channel");
            }
        }
    }

    private void addSettingsToFile() throws IOException {
        if (role2 != null) {
            try {
                FileWriter writer = new FileWriter("src/main/resources/openThreads");
                writer.write(pingChannel.getId() + "-" + role1.getId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                FileWriter writer = new FileWriter("src/main/resources/openThreads");
                writer.write(pingChannel.getId() + "-" + role1.getId() + "-" + role2.getId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


