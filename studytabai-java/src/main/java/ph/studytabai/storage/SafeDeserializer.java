package ph.studytabai.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/**
 * Every deserialization in the app goes through here.
 * Allow-list: our own packages + java.base only; everything else is rejected,
 * plus limits on depth, object count and array size to stop deserialization attacks.
 */
public final class SafeDeserializer {
    private static final String ALLOW = "ph.studytabai.**;java.base/*;!*";
    /** For one-shot reads of files (total size capped). */
    private static final ObjectInputFilter FILE_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=64;maxrefs=200000;maxbytes=20000000;" + ALLOW);
    /** For long-lived network streams (no cumulative byte cap, but per-object limits). */
    private static final ObjectInputFilter NET_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=32;maxrefs=50000;maxarray=100000;" + ALLOW);

    private SafeDeserializer() {}

    public static Object read(InputStream in) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(in)) {
            ois.setObjectInputFilter(FILE_FILTER);
            return ois.readObject();
        }
    }

    /** Opens a filtered ObjectInputStream for a socket connection. */
    public static ObjectInputStream open(InputStream in) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(in);
        ois.setObjectInputFilter(NET_FILTER);
        return ois;
    }
}
