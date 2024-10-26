package Common;

// As classes de request específicas podem ser public já que precisam ser acessadas
// por outras packages (client e server)
public class AuthRequest extends Request {
    public final String username;
    public final String password;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
