import java.io.Serializable;
import java.util.Objects;

public class ClientData implements Serializable {
    String addr;
    int port;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ClientData that = (ClientData) o;
        boolean ret = port == that.port && addr.equals(that.addr);
        return ret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr, port);
    }
}
