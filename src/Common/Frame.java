package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Frame {

    public final int tag;
    public final String stringInput;
    public final byte[] data;

    public Frame(int tag, String stringInput, byte[] data) {
        this.tag = tag;
        this.stringInput = stringInput;
        this.data = data;
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeInt(tag);
        out.writeUTF(stringInput);
        out.writeInt(data.length);
        out.write(data);
    }

    public static Frame deserialize(DataInputStream in) throws IOException {
        int tag = in.readInt();
        String stringInput = in.readUTF();
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readFully(data);
        return new Frame(tag, stringInput, data);
    }
}