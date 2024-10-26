package Common;

import java.util.Map;

public class MultiPutRequest extends Request {
    public final Map<String, byte[]> pairs;

    public MultiPutRequest(Map<String, byte[]> pairs) {
        this.pairs = pairs;
    }
}
