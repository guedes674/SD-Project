package Common;

import java.util.Set;

public class MultiGetRequest extends Request {
    public final Set<String> keys;

    public MultiGetRequest(Set<String> keys) {
        this.keys = keys;
    }
}
