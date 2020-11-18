import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.*;

public class Client {
    //init : output queue, message map, map of all clients (incl comline)
    //not map but list --> <id, message, sendtime, origtime, list <id, clientIP>>
    //message = <msg, reply/message, host>
    //msg = <ID, text>
    //connect Y\N -> choose + connect
    //open inputhandler + outputhandler + inputhandler for client (comlinehandler)
    //loops on accepting + opening new inputhandler

    public static int port;
    public static String name;
    public static int loss;
    public static volatile LocalTime mescheck = null; //time when to check messages map, msg timeout = 15sec
    public static ClientData self = new ClientData();
    public static ClientData secroot = null;
    public static DatagramSocket socket;
    public static ClientData thirdroot = null;
    public static ConcurrentHashMap<UUID, MStruct> list = new ConcurrentHashMap<>(); //GUID - MStruct - list of what we send
    public static BlockingQueue<Message> queue = new LinkedBlockingQueue<>() ; //queue on output
    public static ConcurrentHashMap<UUID, LocalTime> messages = new ConcurrentHashMap<>();//map of <id, lastTimeReceived> not to repeat same message
    public static Vector<ClientData> clients = new Vector<>(); //list of clients
    public static void main(String[] args) {
        try {
            parse(args);
            socket = new DatagramSocket(port);
            //System.out.println(InetAddress.getLocalHost().toString().split("/")[1]);
            self.port = port;
            //self.addr = InetAddress.getLocalHost().toString().split("/")[1];
            self.addr = "0.0.0.0";
            if (args.length > 3) {
                System.out.println(secroot.addr + " " + secroot.port);
                connect(secroot); //parent = emergency root
            }
            OutputHandler.Start();
            InputHandler.Start();
            ComLineHandler.Start();
            System.out.println("ready to wait new messages");
//            while (true) {
//                //Socket client = in.accept();
//                System.out.println("accepted new client");
//                InputHandler.Start(client);
//                ClientData newclient;
//                newclient.addr = ;//?
//                newclient.port = ;//?
//                clients.add(clients.size(), newclient);
//            }
        } catch (IOException e) {
            socket.close();
            e.printStackTrace();
        }
    }
    private static void parse(String[] args){
        name = args[0];
        //System.out.println(args[1] + "  " + args[2]);
        loss = Integer.parseInt(args[2]);
        port = Integer.parseInt(args[1]);
        if (args.length > 3) {
            String addr_sec = args[3];
            int port_sec = Integer.parseInt(args[4]);
            secroot = new ClientData();
            secroot.addr = addr_sec;
            secroot.port = port_sec;
        }
    }
    public static void connect(ClientData cl){
        clients.add(secroot);
        Message tmp = new Message();
        msg tmpacket = new msg();
        tmpacket.cl = cl;//will be sent to everyone except secroot
        tmpacket.id = UUID.randomUUID();
        tmpacket.text = cl.addr + " " + cl.port;
        tmpacket.type = MType.secroot;
        tmp.type = MType.broadcast;
        tmp.packet = tmpacket;
        Client.queue.add(tmp);

        Message tmp_self = new Message();
        msg tmpacket_self = new msg();
        tmpacket_self.cl = cl;//will be sent to secroot
        tmpacket_self.id = UUID.randomUUID();
        tmpacket_self.text = cl.addr + " " + cl.port;
        tmpacket_self.type = MType.secroot_self;
        tmp_self.type = MType.single;
        tmp_self.packet = tmpacket_self;
        Client.queue.add(tmp);
        //System.out.println("secroot was added on sending, id = " + tmp.packet.id);
    }
}
