package Common;

import java.util.Map;

public class MultiGetResponse extends Response {
    public final Map<String, byte[]> values;

    public MultiGetResponse(Map<String, byte[]> values) {
        super(true, "MultiGet successful");
        this.values = values;
    }
}
