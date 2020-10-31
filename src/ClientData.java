import java.io.Serializable;
import java.util.Objects;

public class ClientData implements Serializable {
    String addr;
    int port;

    @Override
    public boolean equals(Object o) {
        //System.out.println("calls equals");
        //if (this == o) return true;
        //System.out.println("not true");
        if (o == null || getClass() != o.getClass()) return false;
        ClientData that = (ClientData) o;
        //System.out.println("that port = " + that.port + " that addr = " + that.addr + " port = " + port + " addr = " + addr);
        boolean ret = port == that.port && addr.equals(that.addr);
        //System.out.println("ret = " + ret);
        return ret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr, port);
    }
}
