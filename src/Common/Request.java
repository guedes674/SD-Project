package Common;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class Request {
    public static final int AUTH = 1;
    public static final int REGISTER = 2;
    public static final int PUT = 3;
    public static final int GET = 4;
    public static final int MULTI_PUT = 5;
    public static final int MULTI_GET = 6;
    public static final int GET_WHEN = 7;
    public static final int LOGOUT = 8;

    public static byte[] serializeMap(Map<String, byte[]> result) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);

        dataStream.writeInt(result.size());
        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            dataStream.writeUTF(entry.getKey());
            byte[] value = entry.getValue();
            if (value != null) {
                dataStream.writeInt(value.length);
                dataStream.write(value);
            } else {
                dataStream.writeInt(0);
            }
        }

        dataStream.flush();
        return byteStream.toByteArray();
    }
}