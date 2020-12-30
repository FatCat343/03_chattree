import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InputHandler implements Runnable {
    public String addr;
    public DatagramSocket client;
    public void run(){
        try {
            System.out.println("start waiting");
            while (true) {
                byte[] buffer = new byte[1024];
                msg packet = new msg();
                packet.receive();
                if (Client.loss > Math.random() * 100) {
                    continue;
                }
                //check if packet address + port is in the list
                if (!Client.clients.contains(packet.cl)) {
                    //first message from client -> new client
                    //adds it to client list + sends secondary root
                    if (Client.secroot == null){ //was not connected to anyone
                        Client.secroot = new ClientData();
                        Client.secroot = packet.cl;
                    }
                    Client.clients.add(packet.cl);
                    Client.connect(Client.secroot);
                }
                //anyway we need to parse received packet's message
                if (packet.type == MType.reply) { //reply on receiving our msg - find it in map + delete us
                    Client.list.get(packet.id).branches.remove(packet.cl);
                } else {
                    if (packet.type == MType.secroot) {
                        if (packet.cl.equals(Client.secroot)) {
                            if (Client.thirdroot == null) Client.thirdroot = new ClientData();
                            Client.thirdroot.port = Integer.parseInt(packet.text.split(" ")[1]);
                            Client.thirdroot.addr = packet.text.split(" ")[0];
                        }
                    }
                    if (packet.type == MType.secroot_self) {
                        if (packet.cl.equals(Client.secroot)) {
                            if (Client.thirdroot == null) Client.thirdroot = new ClientData();
                            Client.thirdroot.port = Client.self.port;
                            Client.thirdroot.addr = Client.self.addr;
                        }
                    }
                    if (packet.type == MType.message){ //message was received
                        if (!Client.messages.containsKey(packet.id)) { //see message first time - print + broadcast mes
                            LocalTime tmp = LocalTime.now();
                            Client.messages.put(packet.id, tmp);
                            if (Client.mescheck == null) Client.mescheck = tmp.plusSeconds(15);

                            System.out.println(packet.text);
                            Message message = new Message();
                            message.packet = new msg();
                            message.packet.cl = new ClientData();
                            message.packet.cl = packet.cl;
                            message.packet.type = MType.message;
                            message.packet.text = packet.text;
                            message.packet.id = UUID.randomUUID();
                            message.type = MType.broadcast;
                            Client.queue.add(message);
                        }
                        else {
                            LocalTime tmp = Client.messages.remove(packet.id);
                            LocalTime newtmp = LocalTime.now();
                            Client.messages.put(packet.id, newtmp);
                        }
                    }
                    //form a reply
                    Message message = new Message();
                    packet.type = MType.reply;
                    message.packet = packet;
                    message.host = addr; //addr of sender of text
                    message.type = MType.single;
                    Client.queue.add(message);
                }
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    public static void Start(){
        Thread t = new Thread(new InputHandler());
        t.start();
    }
}
