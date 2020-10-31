import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InputHandler implements Runnable {
    //init
    //blocks on reading
    //reads -> parses (reply or new msg)
    //if msg forms reply to outputhandler queue + form broadcast of msg to outqueue
    //if reply - set that origmsg was delivered to its partner (delete token from list[] of clients, if list == NULL delete whole message)
    //blocks on reading
    //public int loss;
    public String addr;
    public void run(){
        try {
            while (true) {

                //byte[] byteArray = new byte[8192];
                //int len = input.read(byteArray, 0, 8192); //reads String Name:Text
                //msg mes = new msg();
                byte[] buffer = new byte[1024];
                msg packet = new msg();
                packet.receive();
                if (Client.loss > Math.random() * 100) continue;
                //check if packet address + port is in the list
                if (!Client.clients.contains(packet.cl)) {
                    //first message from client -> new client
                    //adds it to client list + sends secondary root
                    Client.clients.add(packet.cl);
                    Message tmp = new Message();
                    msg tmpacket = new msg();
                    tmpacket.cl = packet.cl;
                    tmpacket.id = UUID.randomUUID();
                    tmpacket.text = Client.secroot.addr + " " + Client.secroot.port;
                    tmpacket.type = MType.secroot;
                    tmp.type = MType.single;
                    Client.queue.add(tmp);
                }
                //anyway we need to parse received packet's message
                if (packet.type == MType.reply) { //reply on receiving our msg - find it in map + delete us
                    Client.list.get(packet.id).branches.remove(packet.cl);
                } else {
                    if (packet.type == MType.secroot) {
                        //second root info - change info about our thirdroot IF ONLY host of msg was our secroot
                        if (packet.cl.equals(Client.secroot)) {
                            Client.thirdroot.port = Integer.parseInt(packet.text.split(" ")[1]);
                            Client.thirdroot.addr = packet.text.split(" ")[0];
                        }
                    } else { //message was received
                        if (!Client.list.containsKey(packet.id)) { //see message first time - print + broadcast mes
                            //ask to print msg
                            //broadcast msg
                            System.out.println(packet.text);
                            Message message = new Message();
                            message.packet = packet;
                            message.packet.id = UUID.randomUUID();
                            //message.host = addr; //addr of sender of text
                            message.type = MType.broadcast;
                            Client.queue.add(message);
                        }
                    }
                    //form a reply
                    Message message = new Message();
                    packet.type = MType.reply;
                    message.packet = packet; //check if GUID stays same
                    message.host = addr; //addr of sender of text
                    message.type = MType.single;
                    Client.queue.add(message);
                }


            }

        }
        catch (IOException | ClassNotFoundException err){
            err.printStackTrace();
        }
    }
    public static void Start(){
        Thread t = new Thread(new InputHandler());
        t.start();
    }
}
