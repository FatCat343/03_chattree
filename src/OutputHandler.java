import java.net.Socket;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

public class OutputHandler implements Runnable {
    public static PriorityQueue<UUID> resend = new PriorityQueue<>();
    public LocalTime invoketime = null;
    public LocalTime checktime = null;
    public void run(){
        while (true) {
            Message message;
            try {
                message = Client.queue.poll(3L, TimeUnit.SECONDS);
                if (message != null) {
                    if (message.packet.type == MType.message) {
                        send_all(message);
                    }
                    if (message.packet.type == MType.secroot) {
                        send_all(message);
                    }
                    if (message.packet.type == MType.reply) { //reply - single
                        message.packet.send(message.packet.cl); //only to packet.cl
                    }
                    if (message.packet.type == MType.check) { //check - single
                        send_single(message);
                    }
                    if (message.packet.type == MType.secroot_self) { //check - single
                        send_single(message);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("polling was interrupted");
                e.printStackTrace();
            }
            while ((invoketime != null) && (SECONDS.between(LocalTime.now(), invoketime) <= 0)) {
                //resends oldest message
                UUID id =resend.poll();
                if (id != null){
                    MStruct tmp = Client.list.get(id);
                    if (SECONDS.between(LocalTime.now(), tmp.origtime.plusSeconds(30)) < 0) {
                        deleteclients(tmp.branches);
                        Client.list.remove(id);
                        //copied from end of while not to miss this part
                        UUID next = resend.peek();
                        if (next != null) {
                            MStruct tmp1 = Client.list.get(next);
                            invoketime = tmp1.sendtime.plusSeconds(3);
                        }
                        else invoketime = null;
                        continue;
                    }
                    if (tmp.branches.size() > 0) {
                        for (int i = 0; i < tmp.branches.size(); i++) {
                            if (Client.clients.contains(tmp.branches.elementAt(i))) tmp.message.packet.send(tmp.branches.elementAt(i));
                        }
                        tmp.sendtime = LocalTime.now();
                        if (tmp.message.packet.type == MType.check) checktime = checktime.plusSeconds(3);
                        resend.add(id); //add to tail
                    }
                    else {
                        Client.list.remove(id);
                    }
                }
                UUID next = resend.peek();
                if (next != null) {
                    MStruct tmp = Client.list.get(next);
                    invoketime = tmp.sendtime.plusSeconds(3);
                }
                else invoketime = null;
            }
            //check secroot on availability by sending our secroot to him
            //once in 30 sec sends test msg to secroot waiting for reply
            //also removes all old messages from messagemap
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
                    //now remove messages
                    Iterator<Map.Entry<UUID, LocalTime>> it = Client.messages.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<UUID, LocalTime> pair = it.next();
                        LocalTime lastseen = pair.getValue();
                        if (lastseen.plusSeconds(15).isBefore(LocalTime.now())) { //timeout
                            it.remove();
                        }
                    }

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
        mst.branches = new Vector<>(Client.clients.size());
        mst.branches.setSize(Client.clients.size());
        Collections.copy(mst.branches, Client.clients);
        mst.branches.remove(mst.message.packet.cl); //remove host of message
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Client.list.put(message.packet.id, mst);
        boolean res = resend.add(message.packet.id);
        if (invoketime == null) {
            invoketime = mst.origtime.plusSeconds(3);
        }
        for (int i = 0; i < Client.clients.size(); i++) {
            if (!message.packet.cl.equals(Client.clients.elementAt(i))) {
                message.packet.send(Client.clients.elementAt(i));
            }
        }
    }
    public void send_single(Message message){
        MStruct mst = new MStruct();
        mst.message = message;
        mst.branches = new Vector<>();
        boolean res1 = mst.branches.add(message.packet.cl);
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Client.list.put(message.packet.id, mst);
        message.packet.send(message.packet.cl);
        boolean res = resend.add(message.packet.id);
        if (invoketime == null) {
            invoketime = mst.origtime.plusSeconds(3);
        }
    }

    public void deleteclients(Vector<ClientData> clients){
        for (int i = 0; i < clients.size(); i++){
            Client.clients.remove(clients.elementAt(i));
            if (clients.elementAt(i).equals(Client.secroot)) {
                if (!Client.thirdroot.equals(Client.self)) {
                    Client.secroot.addr = Client.thirdroot.addr;
                    Client.secroot.port = Client.thirdroot.port;
                    Client.connect(Client.secroot);
                }
                else {
                    if (Client.clients.size() == 0) {
                        Client.secroot = null;
                    }
                    else {
                        Client.secroot.addr = Client.clients.get(0).addr;
                        Client.secroot.port = Client.clients.get(0).port;
                        Client.connect(Client.secroot);
                    }
                }
            }
        }
    }
}
