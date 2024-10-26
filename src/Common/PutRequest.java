package Common;

public class PutRequest extends Request {
    public final String key;
    public final byte[] value;

    public PutRequest(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }
}
