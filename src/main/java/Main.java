import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main( String[] args) throws LoginException, IOException, SQLException {
        ticketNotifier ticketN = new ticketNotifier();
        ticketN.start();
    }
}
