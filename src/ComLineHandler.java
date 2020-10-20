import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;

public class ComLineHandler implements Runnable{
    //same as input but with comandline
    public void run(){
        Scanner in = new Scanner(System.in);
        while (true) {
            String input = in.next();
            String text = "";
            text = text + Client.name + " : " + input;
            msg packet = new msg();
            Message message = new Message();
            packet.cl = null;
            packet.text = text;
            packet.id = UUID.randomUUID();
            packet.type = MType.message;
            message.packet = packet;
            Client.queue.add(message);
        }
    }
    public static void Start(){
        Thread t = new Thread(new ComLineHandler());
        t.start();
    }
}
