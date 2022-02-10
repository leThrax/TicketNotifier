import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.io.IOUtils;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.List;
import java.util.Scanner;


public class ticketNotifier extends ListenerAdapter implements Runnable{

    private static MessageChannel pingChannel = null;
    private static Role role1 = null;
    private static Role role2 = null;
    private static String token;
    private static ReadyEvent readyEvent = null;

    public static void main(String[] args) throws LoginException, IOException {

        try (FileInputStream inputStream = new FileInputStream("src/main/resources/token.txt")) {
            String input = IOUtils.toString(inputStream);
            token = input;
        }

        JDABuilder bot = JDABuilder.createDefault(token);
        bot.setActivity(Activity.watching("Searching for new threads..."));
        bot.addEventListeners(new ticketNotifier());

        //Add commands to the bot
        CommandListUpdateAction commands = bot.build().updateCommands()
                .addCommands(Commands.slash("setup", "Setting up the channel where you want to get pinged and the roles to get pinged.")
                        .addOption(OptionType.CHANNEL, "channel", "Add Channel where the Bot will ping the staff when a new Thread was created", true)
                        .addOption(OptionType.ROLE, "role-1", "Add a Role that is going to be pinged.", true)
                        .addOption(OptionType.ROLE, "role-2", "Add another Role that is going to be pinged.", false));
        commands.queue();

    }


    @Override
    public void onReady (ReadyEvent event) {
        try {
            setupLoadSettings(event);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LoginException e) {
            e.printStackTrace();
        }
        readyEvent = event;
        ticketNotifier thread = new ticketNotifier();
        thread.run();
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
            if (event.getOption("role-2") != null){
                role2 = event.getOption("role-2").getAsRole();
            }
            event.getHook().sendMessage("Finished setting up.").queue();
            if (role2 != null) {
                event.getHook().sendMessage(role1.getName() + " and " + role2.getName() + " will be now pinged in #" + pingChannel.getName()).queue();
            } else {
                event.getHook().sendMessage(role1.getName() + " will be now pinged in #" + pingChannel.getName()).queue();
            }
            try {
                System.out.println("Applying Settings to the File");
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
                        sendPing(event);
                    } else {
                        try {
                            Scanner reader = new Scanner(openThreads);
                            while (reader.hasNextLine()) {
                                long threadID = Long.parseLong(reader.nextLine());
                                if (threadID == event.getThreadChannel().getIdLong()) {
                                    threadExists = true;
                                    reader.close();
                                    break;
                                }
                            }
                            reader.close();
                            if (!threadExists) {
                                sendPing(event);
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
        System.out.println("Writing into settings file...");
        if (role2 == null) {
            try {
                System.out.println(pingChannel.getId() + "-" + role1.getId());
                FileWriter writer = new FileWriter("src/main/resources/settings");
                writer.write(pingChannel.getId() + "-" + role1.getId());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                System.out.println(pingChannel.getId() + "-" + role1.getId() + "-" + role2.getId());
                FileWriter writer = new FileWriter("src/main/resources/settings");
                writer.write(pingChannel.getId() + "-" + role1.getId() + "-" + role2.getId());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void setupLoadSettings(ReadyEvent event) throws IOException, LoginException {
        File settingsFile = new File("src/main/resources/settings");
        if (settingsFile.createNewFile()) {
            System.out.println("Settings file didn't exist, created new one");
        }
        else {
            System.out.println("Settings file exists");
        }
        if (settingsFile.length() != 0) {
            System.out.println("Settings file is not 0");
            Scanner reader = new Scanner(settingsFile);
            String[] setting = reader.nextLine().split("-");
            if (setting.length == 3) {
                pingChannel = event.getJDA().getTextChannelById(setting[0]);
                role1 = event.getJDA().getRoleById(setting[1]);
                role2 = event.getJDA().getRoleById(setting[2]);
                System.out.println("Applied the Ping Channel and 2 Roles");
            } else {
                pingChannel = event.getJDA().getTextChannelById(setting[0]);
                role1 = event.getJDA().getRoleById(setting[1]);
                System.out.println("Applied the Ping Channel and the role");
            }
            reader.close();
        }
    }

    private void sendPing(MessageReceivedEvent event) {
        try {
            FileWriter writer = new FileWriter("src/main/resources/openThreads", true);
            writer.write(Long.toString(event.getThreadChannel().getIdLong()) + "\n");
            if (role2 != null) {
                pingChannel.sendMessage("New thread detected " + role1.getAsMention() + role2.getAsMention()).queue();
            } else {
                pingChannel.sendMessage("New thread detected " + role1.getAsMention()).queue();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkThread () throws IOException {
        System.out.println("Checking opened Threads...");
        boolean threadExists = false;
        File openThreads = new File("src/main/resources/openThreads");
        Scanner reader = new Scanner(openThreads);
        List<ThreadChannel> guildThreadList = readyEvent.getJDA().getThreadChannels();
        File tmpFile = new File("src/main/resources/openThreadsTMP");
        tmpFile.createNewFile();
        FileWriter writer = new FileWriter("src/main/resources/openThreadsTMP");
        System.out.println("Size of List: " + guildThreadList.size());
        for (int i = 0; i < guildThreadList.size(); i++) {
            while (reader.hasNextLine()) {
                String fileID = reader.nextLine();
                //System.out.println("Thread ID from Thread List: " + guildThreadList.get(i).getId() + "; Thread ID from File: " + fileID);
                if (guildThreadList.get(i).getId().equals(fileID)) {
                    threadExists = true;
                    break;
                }
            }
            if (threadExists) {
                System.out.println("Writing existing Thread into TMP file");
                writer.write(String.valueOf(guildThreadList.get(i).getId()) + "\n");
            }
            reader.reset();
        }
        writer.close();
        reader.close();
        openThreads.delete();
        System.out.println("Deleted old file");
        tmpFile.renameTo(openThreads);
        System.out.println("Renamed new file");
    }

    @Override
    public void run() {
        while (true){
            try {
                checkThread();
                Thread.sleep(60000);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}


