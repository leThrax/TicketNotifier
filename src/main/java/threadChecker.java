import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.ReadyEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class threadChecker implements Runnable {
    ReadyEvent readyEvent;
    private static MessageChannel pingChannel = null;

    threadChecker(ReadyEvent event, MessageChannel channel) {
        readyEvent = event;
        pingChannel = channel;
    }

    /***
     * The code that is executed by the thread
     */
    @Override
    public void run() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now) +": Thread started");
        //Infinite loop to execute checkThread function every x-milliseconds
        while (true) {
            try {
                checkThread();
                Thread.sleep(30000);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Checks if a new thread was opened
     * @throws IOException exception
     */
    private void checkThread () throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now) + ": Checking opened Threads...");
        boolean threadExists = false;
        File openThreads = new File("resources/openThreads.txt");
        //Checks if the openThreads.txt already exists
        if (openThreads.createNewFile()) {
            System.out.println(dtf.format(now) + ": Creating new openThreads.txt file");
        }
        Scanner reader = new Scanner(openThreads);
        //List<ThreadChannel> guildThreadList = new ArrayList<>(readyEvent.getJDA().getThreadChannels());
        File tmpFile = new File("resources/openThreadsTMP.txt");
        tmpFile.createNewFile();
        FileWriter writer = new FileWriter("resources/openThreadsTMP.txt");
        System.out.println(dtf.format(now) + ": Thread list: " + readyEvent.getJDA().getThreadChannels());

        //Checks if a thread in the list is archived, if yes, the thread is getting removed from the list.
        for (int j = 0; j < readyEvent.getJDA().getThreadChannels().size(); j++) {
            System.out.println(dtf.format(now) + ": Size of guildThreadList = " + readyEvent.getJDA().getThreadChannels().size());
                while (reader.hasNextLine()) {
                    String fileID = reader.nextLine();
                    System.out.println(dtf.format(now) + ": Current checked thread id against " +
                            readyEvent.getJDA().getThreadChannels().get(j).getId() + " is " + fileID);
                    if (readyEvent.getJDA().getThreadChannels().get(j).getId().equals(fileID) && !readyEvent.getJDA().getThreadChannels().get(j).isArchived()) {
                        //System.out.println(dtf.format(now) + ": Id match!");
                        System.out.println(dtf.format(now) + ": Writing existing Thread into TMP file");
                        writer.write(String.valueOf(readyEvent.getJDA().getThreadChannels().get(j).getId()) + "\n");
                        break;
                    }
                }
                reader.reset();
        }
        writer.close();
        reader.close();
        openThreads.delete();
        System.out.println(dtf.format(now) + ": Deleted old file");
        tmpFile.renameTo(openThreads);
        System.out.println(dtf.format(now) +": Renamed new file");
    }

    /**
     * Setter for the ping channel
     * @param pingChannel ping channel
     */
    public void setPingChannel(MessageChannel pingChannel) {
        threadChecker.pingChannel = pingChannel;
    }
}
