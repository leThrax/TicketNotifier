import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
    public static void main( String[] args) throws LoginException, IOException {
        ticketNotifier ticketN = new ticketNotifier();
        ticketN.start();
    }
}
