import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    public static ClientData secroot;
    public static DatagramSocket socket;
    public static ClientData thirdroot;
    public static ConcurrentHashMap<UUID, MStruct> list = new ConcurrentHashMap<>(); //GUID - MStruct
    public static ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>(); //queue on output
    public static Vector<ClientData> clients = new Vector<>(); //list of clients
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            parse(args);

            if (args.length > 3) {
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
            e.printStackTrace();
        }
    }
    private static void parse(String[] args){
        port = Integer.getInteger(args[2]);
        name = args[0];
        loss = Integer.getInteger(args[1]);
        if (args.length > 3) {
            String addr_sec = args[3];
            int port_sec = Integer.getInteger(args[4]);
            ClientData secroot = new ClientData();
            secroot.addr = addr_sec;
            secroot.port = port_sec;
        }
    }
    public static void connect(ClientData cl){
        Message tmp = new Message();
        msg tmpacket = new msg();
        tmpacket.cl = cl;
        tmpacket.id = UUID.randomUUID();
        tmpacket.text = cl.addr + " " + cl.port;
        tmpacket.type = MType.secroot;
        tmp.type = MType.single;
        Client.queue.add(tmp);
    }
}
