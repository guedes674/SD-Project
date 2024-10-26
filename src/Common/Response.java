package Common;

// Common/Response.java
import java.io.Serializable;
import java.util.*;

public class Response implements Serializable {
    public final boolean success;
    public final String message;

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

