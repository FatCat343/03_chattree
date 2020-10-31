import java.net.Socket;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

public class OutputHandler implements Runnable {
    //waits on message queue or invokes to resend message
    //message -> if message send to all others except host (first check that it is not in the list) -> add to list with send time && origtime = sendtime
    //if reply -> send to destination host
    //compare time.now with InvokeTime -> sleep for diff millisec
    //invokes by time -> removes list[0] -> send list[0] && invoke time = 0 -> ads list[0] to the end of list with new send time
    //invoketime = =0 -> invoketime = list[0].time + 3000ms
    //private static ConcurrentLinkedQueue<MStruct> queue;
    public static PriorityQueue<UUID> resend = new PriorityQueue<>();
    public LocalTime invoketime = null;
    public LocalTime checktime = null;
    public void run(){
        while (true) {
            Message message;
            try {
                message = Client.queue.poll(3L, TimeUnit.SECONDS);
                if (message == null) System.out.println("polled null message");
                if (message != null) {
                    System.out.println("msg type = " +message.packet.type);
                    if (message.packet.type == MType.message) {
                        //System.out.println("send message"+LocalTime.from(invoketime).getSecond());
                        send_all(message);
                    }
                    if (message.packet.type == MType.secroot) {
                        //System.out.println("send secroot"+LocalTime.from(invoketime).getSecond());
                        send_all(message);
                    }
                    if (message.packet.type == MType.reply) { //reply - single
                        //System.out.println("send reply"+LocalTime.from(invoketime).getSecond());
                        message.packet.send(message.packet.cl); //only to packet.cl
                    }
                    if (message.packet.type == MType.check) { //check - single
                        //System.out.println("send reply"+LocalTime.from(invoketime).getSecond());
                        //message.packet.send(message.packet.cl); //only to packet.cl
                        send_single(message);
                    }

                }
            } catch (InterruptedException e) {
                System.out.println("polling was interrupted");
                e.printStackTrace();
            }

            if (invoketime == null) System.out.println("null");
            else System.out.println("seconds == "+SECONDS.between(LocalTime.now(), invoketime));

            while ((invoketime != null) && (SECONDS.between(LocalTime.now(), invoketime) <= 0)) {
                //resends oldest message
                UUID id =resend.poll();
                System.out.println("resend old message, id = " + id + ", resend size = " + resend.size());
                if (id != null){
                    MStruct tmp = Client.list.get(id);
                    //System.out.println("tbs = "+tmp.branches.size());
                    if (SECONDS.between(LocalTime.now(), tmp.origtime.plusSeconds(30)) < 0) {
                        deleteclients(tmp.branches);
                        Client.list.remove(id);
                        //copied from end of while not to miss this part
                        UUID next = resend.peek();
                        if (next != null) {
                            MStruct tmp1 = Client.list.get(next);
                            invoketime = tmp1.sendtime.plusSeconds(3);
                            System.out.println(invoketime.getSecond() - tmp1.sendtime.getSecond());
                        }
                        else invoketime = null;
                        continue;
                    }
                    //System.out.println("tbs = "+tmp.branches.size());
                    if (tmp.branches.size() > 0) {
                        for (int i = 0; i < tmp.branches.size(); i++) {
                            if (Client.clients.contains(tmp.branches.elementAt(i))) tmp.message.packet.send(tmp.branches.elementAt(i));
                        }
                        tmp.sendtime = LocalTime.now();
                        System.out.println("id was added back");
                        resend.add(id); //add to tail
                    }
                    else {
                        Client.list.remove(id);
                        System.out.println("removes from list id = " + id);
                    }
                }
                UUID next = resend.peek();
                if (next != null) {
                    MStruct tmp = Client.list.get(next);
                    invoketime = tmp.sendtime.plusSeconds(3);
                    System.out.println(invoketime.getSecond() - tmp.sendtime.getSecond());
                }
                else invoketime = null;
            }

            //check secroot on availability by sending our secroot to him
            //once in 30 sec sends test msg to secroot waiting for reply
            if (Client.secroot != null) {
                if (checktime == null) checktime = LocalTime.now();
                if (SECONDS.between(LocalTime.now(), checktime.plusSeconds(30)) < 0) {
                    Message tmp = new Message();
                    msg tmpacket = new msg();
                    tmpacket.id = UUID.randomUUID();
                    tmpacket.text = Client.secroot.addr + " " + Client.secroot.port;
                    tmpacket.type = MType.check;
                    tmpacket.cl = new ClientData();
                    tmpacket.cl = Client.secroot;
                    tmp.type = MType.single;
                    tmp.packet = tmpacket;
                    Client.queue.add(tmp);
                    checktime = LocalTime.now();
                    System.out.println("Checking secroot was added on sending with id = " + tmp.packet.id);
                }
            }

        }
    }
    public static void Start(){
        Thread t = new Thread(new OutputHandler());
        t.start();
    }
    public void send_all(Message message){
        MStruct mst = new MStruct();
        mst.message = message;
        //mst.branches = Client.clients;
        mst.branches = new Vector<>(Client.clients.size());
        mst.branches.setSize(Client.clients.size());
        Collections.copy(mst.branches, Client.clients);
        //System.out.println("branches size = " + mst.branches.size());
        mst.branches.remove(mst.message.packet.cl); //remove host of message
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Client.list.put(message.packet.id, mst);
        boolean res = resend.add(message.packet.id);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            //System.out.println("adding 3 sec");
            invoketime = mst.origtime.plusSeconds(3);
            //System.out.println(invoketime.getSecond() - mst.origtime.getSecond());
        }
        for (int i = 0; i < Client.clients.size(); i++) {
            if (!message.packet.cl.equals(Client.clients.elementAt(i))) {
                //System.out.println("NOT EQUALS at i = "+ i);
                message.packet.send(Client.clients.elementAt(i));
            }
            //else System.out.println("EQUALS at i = "+ i);
        }
    }
    public void send_single(Message message){
        MStruct mst = new MStruct();
        mst.message = message;
        //mst.branches = Client.clients;
        mst.branches = new Vector<>();
        //Collections.copy(mst.branches, Client.clients);
        boolean res1 = mst.branches.add(message.packet.cl);
        System.out.println("sending check msg to "+message.packet.cl.port+ " status - " + res1);
        //System.out.println("branches size = " + mst.branches.size());
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Client.list.put(message.packet.id, mst);
        message.packet.send(message.packet.cl);
        boolean res = resend.add(message.packet.id);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            //System.out.println("adding 3 sec");
            invoketime = mst.origtime.plusSeconds(3);
            //System.out.println(invoketime.getSecond() - mst.origtime.getSecond());
        }
    }

    public void deleteclients(Vector<ClientData> clients){
        System.out.println("delete clients called");
        for (int i = 0; i < clients.size(); i++){
            Client.clients.remove(clients.elementAt(i));
            if (clients.elementAt(i).equals(Client.secroot)) {
                if (!Client.thirdroot.equals(Client.self))Client.connect(Client.thirdroot);
            }
        }
    }
}
