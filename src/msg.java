import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class msg implements Serializable {
    public String text; //name + message
    public int type; //0 - message, 1 - reply, 2 - secroot
    public UUID id; //unique Id of packet
    public ClientData cl; //IP + port diff from ours
    public void receive() throws IOException, ClassNotFoundException {


        try {
            byte[] recvBuf = new byte[5000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            Client.socket.receive(packet);
            int byteCount = packet.getLength();
            ByteArrayInputStream byteStream = new ByteArrayInputStream(recvBuf);
            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
            Object o = is.readObject();
            is.close();
            msg tmp = (msg)o;
            this.id = tmp.id;
            this.text = tmp.text;
            this.type = tmp.type;
        }
        catch (IOException e) {
            System.err.println("Exception:  " + e);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
    }
    public void send(ClientData cld) {
//        msg tmp = new msg();
//        tmp.id = this.id;
//        tmp.text = this.text;
//        tmp.type = this.type;
//        out.writeObject(tmp);
//        out.flush();
        try {
            InetAddress address = InetAddress.getByName(cld.addr);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            os.flush();
            os.writeObject(this);
            os.flush();
            //retrieves byte array
            byte[] sendBuf = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, address, cld.port);
            int byteCount = packet.getLength();
            Client.socket.send(packet);
            os.close();
        }
        catch (UnknownHostException e) {
            System.err.println("Exception:  " + e);
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
