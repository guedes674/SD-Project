package Common;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FrameList extends ArrayList<Frame> {

    public void serialize(DataOutputStream out) throws IOException {
        out.writeInt(this.size());
        for (Frame frame : this) {
            frame.serialize(out);
        }

    }

    public static FrameList deserialize(DataInputStream in) throws IOException {
        FrameList frameList = new FrameList();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            frameList.add(Frame.deserialize(in));
        }
        return frameList;
    }

}
