import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.ReadyEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        List<ThreadChannel> guildThreadList = readyEvent.getJDA().getThreadChannels();
        File tmpFile = new File("resources/openThreadsTMP.txt");
        tmpFile.createNewFile();
        FileWriter writer = new FileWriter("resources/openThreadsTMP.txt");
        //Gets a list of current thread ids from the server and check it with the ones in the text file
        for (int i = 0; i < guildThreadList.size(); i++) {
            while (reader.hasNextLine()) {
                String fileID = reader.nextLine();
                if (guildThreadList.get(i).getId().equals(fileID)) {
                    threadExists = true;
                    break;
                }
            }
            if (threadExists) {
                System.out.println(dtf.format(now) + ": Writing existing Thread into TMP file");
                writer.write(String.valueOf(guildThreadList.get(i).getId()) + "\n");
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
