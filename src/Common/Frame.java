package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Frame {

    public final int tag;
    public final Map<String, byte[]> keyValuePairs;

    public Frame(int tag, Map<String, byte[]> keyValuePairs) {
        this.tag = tag;
        this.keyValuePairs = Collections.unmodifiableMap(new HashMap<>(keyValuePairs));
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeInt(tag);
        out.writeInt(keyValuePairs.size());
        for (Map.Entry<String, byte[]> entry : keyValuePairs.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
    }

    public static Frame deserialize(DataInputStream in) throws IOException {
        int tag = in.readInt();
        int mapSize = in.readInt();
        Map<String, byte[]> keyValuePairs = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            String key = in.readUTF();
            int valueLength = in.readInt();
            byte[] value = new byte[valueLength];
            in.readFully(value);
            keyValuePairs.put(key, value);
        }
        return new Frame(tag, keyValuePairs);
    }

    // toString method for debugging, transform map of byte[] to map of String
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Frame{tag=").append(tag).append(", keyValuePairs={");
        for (Map.Entry<String, byte[]> entry : keyValuePairs.entrySet()) {
            sb.append(entry.getKey()).append("=");
            sb.append(new String(entry.getValue())).append(", ");
        }
        sb.append("}}");
        return sb.toString();
    }
}