import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.ReadyEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class threadChecker implements Runnable {
    ReadyEvent readyEvent;
    private static MessageChannel pingChannel = null;

    threadChecker(ReadyEvent event, MessageChannel channel) {
        readyEvent = event;
        pingChannel = channel;
        //Thread thread = new Thread(this);
        //thread.start();
    }
        @Override
        public void run() {
        System.out.println("Thread started");
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


    private void checkThread () throws IOException {
        System.out.println("Checking opened Threads...");
        boolean threadExists = false;
        File openThreads = new File("resources/openThreads.txt");
        if (openThreads.createNewFile()) {
            System.out.println("Creating new openThreads.txt file");
        }
        Scanner reader = new Scanner(openThreads);
        List<ThreadChannel> guildThreadList = readyEvent.getJDA().getThreadChannels();
        File tmpFile = new File("resources/openThreadsTMP.txt");
        tmpFile.createNewFile();
        FileWriter writer = new FileWriter("resources/openThreadsTMP.txt");

        for (int i = 0; i < guildThreadList.size(); i++) {
            while (reader.hasNextLine()) {
                String fileID = reader.nextLine();
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

    public void setPingChannel(MessageChannel pingChannel) {
        threadChecker.pingChannel = pingChannel;
    }
}
