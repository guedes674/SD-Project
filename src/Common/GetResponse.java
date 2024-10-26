package Common;

public class GetResponse extends Response {
    public final byte[] value;

    public GetResponse(byte[] value) {
        super(true, "Get successful");
        this.value = value;
    }
}
