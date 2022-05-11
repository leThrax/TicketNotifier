import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.io.IOUtils;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;


public class ticketNotifier extends ListenerAdapter{

    private static MessageChannel pingChannel = null;
    private static Role role1 = null;
    private static Role role2 = null;
    private static String token;
    private static ReadyEvent readyEvent = null;
    private static boolean raidmode = false;
    private static MessageChannel logChannel =  null;
    static threadChecker threadChecker;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();

    public static void start() throws LoginException, IOException {

        //Reads in the token
        try {
            File tokenFile = new File("resources/token.txt");
            Scanner reader = new Scanner(tokenFile);
            token = reader.nextLine();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        //The bot object
        JDABuilder bot = JDABuilder.createDefault(token);
        bot.setActivity(Activity.watching("Searching for new threads..."));
        bot.addEventListeners(new ticketNotifier());
        bot.enableIntents(GatewayIntent.GUILD_MEMBERS);

                //Add commands to the bot
        CommandListUpdateAction commands = bot.build().updateCommands()
                .addCommands(Commands.slash("setup", "Setting up the channel where you want to get pinged and the roles to get pinged.")
                        .addOption(OptionType.CHANNEL, "channel", "Add Channel where the Bot will ping the staff when a new Thread was created", true)
                        .addOption(OptionType.ROLE, "role-1", "Add a Role that is going to be pinged.", true)
                        .addOption(OptionType.ROLE, "role-2", "Add another Role that is going to be pinged.", false));
        commands.addCommands(Commands.slash("clear", "Clears the amount of set messages in a channel.")
                        .addOption(OptionType.INTEGER, "amount", "The amount of messages going to be deleted", true));
        commands.addCommands(Commands.slash("raid-mode", "Enable raidmode to prevent people joining the server")
                        .addOption(OptionType.STRING, "true-false", "Type in true of false to enable or disable raidmode", true));
        commands.queue();

    }

    /**
     * When the bot is ready it loads in the settings and runs a thread that constantly checks a server of Threads
     * @param event Ready event
     */
    @Override
    public void onReady (ReadyEvent event) {
        System.out.println("Bot is ready");
        try {
            setupLoadSettings(event);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LoginException e) {
            e.printStackTrace();
        }
        readyEvent = event;
        Thread thread = new Thread(threadChecker = new threadChecker(readyEvent, null));
        thread.start();
        logChannel = event.getJDA().getTextChannelById("591025614212038666");
    }

    /**
     * Logic of the Slash Command.
     * Will be automatically called when a Slash-Command is executed.
     *
     * @param event the Slash Command event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        //Event /setup
        if (event.getName().equals("setup")) {
            event.deferReply().queue();
            if (isAdmin(event)) {
                //set local variables
                pingChannel = event.getOption("channel").getAsTextChannel();
                threadChecker.setPingChannel(pingChannel);
                role1 = event.getOption("role-1").getAsRole();
                //When 2 roles got selected
                if (event.getOption("role-2") != null) {
                    role2 = event.getOption("role-2").getAsRole();
                }
                event.getHook().sendMessage("Finished setting up.").queue();
                //When 2 roles got selected
                if (role2 != null) {
                    event.getHook().sendMessage(role1.getName() + " and " + role2.getName()
                            + " will be now pinged in <#" + pingChannel.getId() + ">").queue();
                //When 1 role got selected
                } else {
                    event.getHook().sendMessage(role1.getName()
                            + " will be now pinged in <#" + pingChannel.getId() + ">").queue();
                }
                //Add settings to the file to make them persistent
                try {
                    System.out.println(dtf.format(now) +": Applying Settings to the File");
                    addSettingsToFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //Error message
            else {
                event.getHook().sendMessage("Error. You need admin rights to use this command").queue();
            }
        }
        //Event /clear
        if (event.getName().equals("clear")) {
            event.deferReply().queue();
            if (isAdmin(event)) {
                int amount = Integer.valueOf(event.getOption("amount").getAsString());
                if (amount == 0 || amount > 100 || amount < 0) {
                    event.getHook().sendMessage("Please enter a valid integer between 1 and 100").queue();
                }
                else {
                    MessageChannel commandChannel = event.getChannel();
                    //Retrieves the messages of a set amount, then deletes them.
                    commandChannel.getHistory().retrievePast(amount)
                            .queue(messages -> { commandChannel.purgeMessages(messages); });
                    /*
                   File deletedMessageLog = new File("resources/deletedMessages.txt");
                    try {
                        if(deletedMessageLog.createNewFile()) {
                            System.out.println(dtf.format(now) +": Created deletedMessages.txt File");
                        }

                        FileWriter dMWriter = new FileWriter("resources/deletedMessages.txt", true);
                        commandChannel.getHistory().retrievePast(amount).queue(messages -> {
                            try {
                                writeLog(amount, messages, dMWriter);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                        this.wait(100);
                        logChannel.sendFile(deletedMessageLog).queue();
                        deletedMessageLog.delete();

                    } catch (IOException e) {

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } */

                    event.getHook().sendMessage("Successfully cleared " + amount + " messages").queue();
                }
            }
            else {
                event.getHook().sendMessage("Error. You need admin rights to use this command").queue();
            }
        }
        //Event /raid-mode
        if (event.getName().equals("raid-mode")) {
            event.deferReply().queue();
            if (isAdmin(event)) {
                if (event.getOption("true-false").getAsString().equals("true") || event.getOption("true-false").getAsString().equals("false")) {
                    raidmode = Boolean.parseBoolean(event.getOption("true-false").getAsString());
                    if (raidmode == true) {
                        event.getHook().sendMessage("Raidmode successfully activated").queue();
                        logChannel.sendMessage("Raidmode successfully activated").queue();
                    }
                    else {
                        event.getHook().sendMessage("Raidmode successfully deactivated").queue();
                        logChannel.sendMessage("Raidmode successfully deactivated").queue();
                    }
                }
                else {
                    event.getHook().sendMessage("Please write true to activate raidmode or false to deactivate raidmode").queue();
                }
            }
        }
    }

    /**
     * Logic, when a Message got received
     * Will be automatically called
     *
     * @param event Message received
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        boolean threadExists = false;

        //When the user is not a bot
        if (event.getMessage().getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
            //When the ping Channel is set
            if (pingChannel != null) {
                //When the message is from a Discord thread
                if (event.isFromThread()) {
                    File openThreads = new File("resources/openThreads.txt");
                    try {
                        //If the file openThreads.txt does not exist, create a new one
                        if(openThreads.createNewFile()) {
                            System.out.println(dtf.format(now) +": Created openThreads.txt File");
                        }
                        if (openThreads.length() == 0) {
                            //Sends the ping message that a thread was found and write thread id into openThreads.txt
                            sendPing(event);
                        } else {
                            try {
                                Scanner reader = new Scanner(openThreads);
                                //Checks if the thread id is already in the file
                                while (reader.hasNextLine()) {
                                    long threadID = Long.parseLong(reader.nextLine());
                                    if (threadID == event.getThreadChannel().getIdLong()) {
                                        threadExists = true;
                                        reader.close();
                                        break;
                                    }
                                }
                                reader.close();
                                /*  If the thread does not exist in the list,
                                * send a ping and write the id into openThreads.txt
                                */
                                if (!threadExists) {
                                    sendPing(event);
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println(dtf.format(now) +": Error, please execute /setup first and select a channel");
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        System.out.println(dtf.format(now) +"New user joined the server: " + event.getUser());
        if (raidmode == true) {
            Guild server = event.getGuild();
            server.kick(event.getMember()).queue();
            event.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("Automated bot message: " +
                    event.getGuild().getName() + " has raidmode activated. Please join back later.")).queue();
            logChannel.sendMessage("**RAIDMODE**: Kicked user " + event.getUser().getName()+ "("
                    + event.getUser().getId() + ")" ).queue();
        }
    }

    /**
     * Adds the settings to a file to make them persistent
     * @throws IOException exception
     */
    private void addSettingsToFile() throws IOException {
        System.out.println(dtf.format(now) +": Writing into settings.txt file...");
        //For one selected role
        if (role2 == null) {
            try {
                System.out.println(pingChannel.getId() + "-" + role1.getId());
                FileWriter writer = new FileWriter("resources/settings.txt");
                writer.write(pingChannel.getId() + "-" + role1.getId());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        //For two selected roles
        } else {
            try {
                System.out.println(pingChannel.getId() + "-" + role1.getId() + "-" + role2.getId());
                FileWriter writer = new FileWriter("resources/settings.txt");
                writer.write(pingChannel.getId() + "-" + role1.getId() + "-" + role2.getId());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads the setup settings and loads the roles and channel where those get pinged or creates new file
     * @param event ready event
     * @throws IOException exception
     * @throws LoginException excpetion
     */
    private static void setupLoadSettings(ReadyEvent event) throws IOException, LoginException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        File settingsFile = new File("resources/settings.txt");
        //when the file doesn't exist
        if (settingsFile.createNewFile()) {
            System.out.println(dtf.format(now) +": Settings file didn't exist, created new one");
        }
        else {
            System.out.println(dtf.format(now) +": Settings file exists");
        }
        //When the file is not empty
        if (settingsFile.length() != 0) {
            System.out.println(dtf.format(now) +": Settings file is not 0");
            Scanner reader = new Scanner(settingsFile);
            String[] setting = reader.nextLine().split("-");
            //When 2 roles are stored
            if (setting.length == 3) {
                pingChannel = event.getJDA().getTextChannelById(setting[0]);
                threadChecker.setPingChannel(pingChannel);
                role1 = event.getJDA().getRoleById(setting[1]);
                role2 = event.getJDA().getRoleById(setting[2]);
                System.out.println(dtf.format(now) +": Applied the Ping Channel and 2 Roles");
            //when 1 role is stored
            } else {
                pingChannel = event.getJDA().getTextChannelById(setting[0]);
                role1 = event.getJDA().getRoleById(setting[1]);
                System.out.println(dtf.format(now) +": Applied the Ping Channel and the role");
            }
            reader.close();
        }
    }

    /**
     * Sends a ping to the set channel when a new thread is created
     * @param event message received event
     */
    private void sendPing(MessageReceivedEvent event) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        try {
            FileWriter writer = new FileWriter("resources/openThreads.txt", true);
            writer.write(Long.toString(event.getThreadChannel().getIdLong()) + "\n");
            System.out.println(dtf.format(now) +": New Thread ID written into openThreads.txt File");
            //When 2 roles are set
            if (role2 != null) {
                pingChannel.sendMessage("New thread " + "<#" + event.getThreadChannel().getIdLong() + ">" + " detected " + role1.getAsMention() + role2.getAsMention()).queue();
            //When 1 role is set
            } else {
                pingChannel.sendMessage("New thread " + "<#" + event.getThreadChannel().getIdLong() + ">" + " detected " + role1.getAsMention()).queue();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the user of the slash command has Discord Administrator privileges
     * @param event slash command event
     * @return boolean
     */
    private boolean isAdmin(SlashCommandInteractionEvent event) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        List<Role> userRolesList = event.getInteraction().getMember().getRoles();
        //Iterates through the roles list of the slash command user
        for (int i = 0; i < userRolesList.size(); i++) {
            if (userRolesList.get(i).getPermissions().contains(Permission.ADMINISTRATOR) || userRolesList.get(i).getId().equals("470749380325146625")) {
                System.out.println(dtf.format(now) +": Slash Command User role has admin rights");
                return true;
            }
        }
        return false;
    }

    private void writeLog(int amount, List<Message> messages, FileWriter dMWriter) throws IOException {
        for (int i = 0;  i < amount; i++) {
            dMWriter.append(messages.get(i).getContentRaw()+ "\n");
        }
        dMWriter.close();
    }

}


