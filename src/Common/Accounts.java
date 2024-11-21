package Common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Allows us to efficiently store and access users' credentials.
 */
public class Accounts implements Serializable {
    private final HashMap<String, String> credentialsMap;
    public ReentrantReadWriteLock l = new ReentrantReadWriteLock();

    public Accounts() {
        this.credentialsMap = new HashMap<>();
    }

    /**
     * Fetches a user's password.
     * 
     * @param email the user's email address.
     * @return the user's password, or <i>null</i> if the user is not registered in
     *         the system.
     */
    public String getPassword(String email) {
        return credentialsMap.get(email);
    }

    /**
     * Adds a new account.
     * 
     * @param email    the user's email address.
     * @param password the user's password.
     */
    public void addAccount(String email, String password) {
        credentialsMap.put(email, password);
    }

    /**
     * Checks if an account exists.
     * 
     * @param email the user's email address.
     * @return <i>true</i> if the account exists, <i>false</i> otherwise.
     */
    public boolean accountExists(String email) {
        return credentialsMap.containsKey(email);
    }
}