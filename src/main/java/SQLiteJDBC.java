import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class SQLiteJDBC {
    Connection c = null;
    DateTimeFormatter dtf;
    LocalDateTime now;
    public void start() {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        try {
            Class.forName("org.sqlite.JDBC");
            //c = DriverManager.getConnection("jdbc:sqlite:D:\\IntelliJ Projects\\resources\\openThread.db");
            c = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.dir")+
                    "\\resources\\openThread.db");
            c.setAutoCommit(false);
            } catch (Exception e) {
                System.err.println( e.getClass().getName() + ": " + e.getMessage() );
                System.exit(0);
            }

            System.out.println(dtf.format(now) + ": Opened database successfully");
        }

    public void createTables (Connection c) throws SQLException {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        try {
            Statement stmt = null;
            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS THREADS " +
                    "(ID TEXT PRIMARY KEY NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
        } catch ( Exception e) {
            System.err.println(e.getClass() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println(dtf.format(now) + ": Table created successfully");

    }

    public void insert(Connection c, String threadID) throws SQLException {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        try {
            Statement stmt = c.createStatement();
            String sql = "INSERT INTO THREADS (ID) " +
                    "VALUES (" + threadID + ");";
            stmt.executeUpdate(sql);
            stmt.close();
            c.commit();
        } catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println(dtf.format(now) + ": Record " + threadID + " created successfully");
        printDB(c);
    }

    public void delete(Connection c, String threadID) throws SQLException {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        try {
            Statement stmt = c.createStatement();
            String sql = "DELETE FROM THREADS WHERE ID=" +
                    threadID + ";";
            stmt.executeUpdate(sql);
            stmt.close();
            c.commit();
        } catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println(dtf.format(now) + ": Record " +  threadID + " deleted successfully");
        printDB(c);
    }

    public boolean checkOpenedThreads (Connection c, String threadID) throws SQLException {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        boolean exists = false;
        try {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM THREADS;");
            while (rs.next()) {
                String id = rs.getString("ID");
                if (id.equals(threadID)) {
                    //System.out.println(dtf.format(now) + ": Thread " + threadID + " is already open");
                    exists = true;
                    break;
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return exists;
    }

    public void printDB(Connection c) throws SQLException {
        dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        now = LocalDateTime.now();

        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM THREADS;");
        System.out.println(dtf.format(now) + ": Current entries in DB");
        while(rs.next()) {
            String id = rs.getString("ID");
            System.out.println("ID = " + id);
        }
        rs.close();
        stmt.close();
    }

    public Connection getConnection () {
        return c;
    }
}
