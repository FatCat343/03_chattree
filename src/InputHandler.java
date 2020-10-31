import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    public DatagramSocket client;
    //private static ConcurrentLinkedQueue<Message> queue;
    //private static ConcurrentHashMap<Integer, MStruct> list; //GUID-MStruct
    public void run(){
        try {
            System.out.println("start waiting");
            while (true) {
                //byte[] byteArray = new byte[8192];
                //int len = input.read(byteArray, 0, 8192); //reads String Name:Text
                //msg mes = new msg();
                byte[] buffer = new byte[1024];
                msg packet = new msg();
                packet.receive();
                System.out.println("received new packet");
                if (Client.loss > Math.random() * 100) {
                    System.out.println("input was dropped");
                    continue;
                }
                //check if packet address + port is in the list
                if (!Client.clients.contains(packet.cl)) {
                    //first message from client -> new client
                    //adds it to client list + sends secondary root
                    //System.out.println("clients len = " + Client.clients.size());
                    System.out.println("new client, port = "+packet.cl.port + ", addr = "+ packet.cl.addr);
                    Client.clients.add(packet.cl);
                    //if (!Client.clients.contains(packet.cl)) System.out.println("adding failed");
                    //else System.out.println("clients len = " + Client.clients.size());
                    Message tmp = new Message();
                    msg tmpacket = new msg();
                    tmpacket.cl = packet.cl;
                    tmpacket.id = UUID.randomUUID();
//                    if (Client.secroot != null) tmpacket.text = Client.secroot.addr + " " + Client.secroot.port;
//                    else {
//                        Client.secroot = packet.cl;
//                        Client.thirdroot = packet.cl;
//                    }
                    if (Client.secroot == null){ //was not connected to anyone
                        Client.secroot = new ClientData();
                        Client.secroot = packet.cl;
                    }
                    if (Client.thirdroot == null) {
                        Client.thirdroot = new ClientData();
                        Client.thirdroot.addr = packet.text.split(" ")[0];
                        Client.thirdroot.port = Integer.parseInt(packet.text.split(" ")[1]);
                    }
                    tmpacket.text = Client.secroot.addr + " " + Client.secroot.port;
                    tmpacket.type = MType.secroot;
                    tmp.packet = tmpacket;
                    tmp.packet.id = UUID.randomUUID();
                    tmp.packet.cl = Client.self;
                    //System.out.println("adding tmp with output addr ="+tmp.packet.cl.addr);
                    tmp.type = MType.single;
                    System.out.println("creating new message with secroot with id =" + tmp.packet.id + " to port = " + tmp.packet.cl.port + " addr = " + tmp.packet.cl.addr);
                    Client.queue.add(tmp);
                }
                //anyway we need to parse received packet's message
                if (packet.type == MType.reply) { //reply on receiving our msg - find it in map + delete us
                    System.out.println("received reply with id =" + packet.id + " port = " + packet.cl.port + "addr = " + packet.cl.addr + "brsize was = "+ Client.list.get(packet.id).branches.size());
                    Client.list.get(packet.id).branches.remove(packet.cl);
                    System.out.println("brsize is = "+ Client.list.get(packet.id).branches.size());
                } else {
                    if (packet.type == MType.secroot) {
                        //second root info - change info about our thirdroot IF ONLY host of msg was our secroot
                        System.out.println("received secroot message, GUID = " + packet.id + " port = " + packet.cl.port + "addr = " + packet.cl.addr);
                        if (packet.cl.equals(Client.secroot)) {
                            System.out.println("   from ours secroot, changes thirdroot to " + packet.text.split(" ")[0] + " " + Integer.parseInt(packet.text.split(" ")[1]));
                            //System.out.println("change from " + Client.thirdroot.port + " to " + Integer.parseInt(packet.text.split(" ")[1]));
                            if (Client.thirdroot == null) Client.thirdroot = new ClientData();
                            Client.thirdroot.port = Integer.parseInt(packet.text.split(" ")[1]);
                            Client.thirdroot.addr = packet.text.split(" ")[0];
                        }
                    }
                    if (packet.type == MType.message){ //message was received
                        System.out.println("message received, id = " + packet.id + " port = " + packet.cl.port + "addr = " + packet.cl.addr);
                        if (!Client.list.containsKey(packet.id)) { //see message first time - print + broadcast mes
                            //ask to print msg
                            //broadcast msg to all others
                            System.out.println(packet.text);
                            Message message = new Message();
                            message.packet = new msg();
                            message.packet.cl = new ClientData();
                            //System.out.println("change from "+ message.packet.cl.port + " to " + packet.cl.port);
                            message.packet.cl = packet.cl;
                            message.packet.type = MType.message;
                            message.packet.text = packet.text;
                            message.packet.id = UUID.randomUUID(); //destroys packet id to form reply!!!!!
                            //message.host = addr; //addr of sender of text
                            message.type = MType.broadcast;
                            System.out.println("creating new message with message with id =" + message.packet.id + " to port = " + message.packet.cl.port + " addr = " + message.packet.cl.addr);
                            Client.queue.add(message);
                        }
                    }
                    if (packet.type == MType.check) { //check was received
                        System.out.println("Check message received, id = " + packet.id);
                    }
                    //form a reply
                    Message message = new Message();
                    packet.type = MType.reply;
                    message.packet = packet; //check if GUID stays same
                    System.out.println("forms reply with id =" + message.packet.id);
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
