import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.ReadyEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class threadChecker implements Runnable {
    ReadyEvent readyEvent;
    private static MessageChannel pingChannel = null;
    static SQLiteJDBC db;
    static Connection c = null;
    DateTimeFormatter dtf;
    LocalDateTime now;

    threadChecker(ReadyEvent event, MessageChannel channel, SQLiteJDBC sqLiteJDBC) {
        readyEvent = event;
        pingChannel = channel;
        db = sqLiteJDBC;
        c = db.getConnection();
    }

    /***
     * The code that is executed by the thread
     */
    @Override
    public void run() {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();
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
            } catch (Exception e) {
            }
        }
    }


    /**
     * Checks if a new thread was opened
     * @throws IOException exception
     */
    private void checkThread () throws IOException, SQLException {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        System.out.println(dtf.format(now) + ": Checking opened Threads...");
        boolean threadExists = false;

        //System.out.println(dtf.format(now) + ": Thread list: " + readyEvent.getJDA().getThreadChannels());

        //Checks if a thread in the list is archived, if yes, the thread is getting removed from the list.
        for (int i = 0; i < readyEvent.getJDA().getThreadChannels().size(); i++) {
            if(db.checkOpenedThreads(c, readyEvent.getJDA().getThreadChannels().get(i).getId())
                    && readyEvent.getJDA().getThreadChannels().get(i).isArchived()) {
                System.out.println(dtf.format(now) + ": Thread " + readyEvent.getJDA().getThreadChannels().get(i).getId()
                                + " is archived, removing...");
                db.delete(c, readyEvent.getJDA().getThreadChannels().get(i).getId());
                threadExists = true;
            }
        }
        if (!threadExists) {
            System.out.println(dtf.format(now) + ": No new archived threads found");
        }
    }

    /**
     * Setter for the ping channel
     * @param pingChannel ping channel
     */
    public void setPingChannel(MessageChannel pingChannel) {
        threadChecker.pingChannel = pingChannel;
    }
}
