import java.net.Socket;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OutputHandler implements Runnable {
    //waits on message queue or invokes to resend message
    //message -> if message send to all others except host (first check that it is not in the list) -> add to list with send time && origtime = sendtime
    //if reply -> send to destination host
    //compare time.now with InvokeTime -> sleep for diff millisec
    //invokes by time -> removes list[0] -> send list[0] && invoke time = 0 -> ads list[0] to the end of list with new send time
    //invoketime = =0 -> invoketime = list[0].time + 3000ms
    //private static ConcurrentLinkedQueue<MStruct> queue;
    public static Queue<UUID> resend = new Queue<UUID>() {
        @Override
        public boolean add(UUID uuid) {
            return false;
        }

        @Override
        public boolean offer(UUID uuid) {
            return false;
        }

        @Override
        public UUID remove() {
            return null;
        }

        @Override
        public UUID poll() {
            return null;
        }

        @Override
        public UUID element() {
            return null;
        }

        @Override
        public UUID peek() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<UUID> iterator() {
            return null;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends UUID> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {

        }
    };
    public LocalTime invoketime = null;
    public void run(){
        while (true) {
            Message message = Client.queue.poll();
            if (message != null) {
                if (message.packet.type == MType.message) send_all(message);
                if (message.packet.type == MType.secroot) send_all(message);
                if (message.packet.type == MType.reply) { //reply - single
                    message.packet.send(message.packet.cl); //only to packet.cl
                }
            }
            while ((invoketime != null) && (LocalTime.now().getNano() >= invoketime.getNano())) {
                //resends oldest message
                UUID id = resend.poll();
                if (id != null){
                    MStruct tmp = Client.list.get(id);
                    if (tmp.origtime.plusSeconds(30).getNano() < LocalTime.now().getNano()) {
                        deleteclients(tmp.branches);
                    }
                    if (tmp.branches.size() > 0) {
                        for (int i = 0; i < tmp.branches.size(); i++) {
                            if (Client.clients.contains(tmp.branches.elementAt(i))) tmp.message.packet.send(tmp.branches.elementAt(i));
                        }
                        tmp.sendtime = LocalTime.now();
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

        }
    }
    public static void Start(){
        Thread t = new Thread(new OutputHandler());
        t.start();
    }
    public void send_all(Message message){
        for (int i = 0; i < Client.clients.size(); i++) {
            if (!message.packet.cl.equals(Client.clients.elementAt(i))) message.packet.send(Client.clients.elementAt(i));
        }
        MStruct mst = new MStruct();
        mst.message = message;
        mst.branches = Client.clients;
        mst.branches.remove(mst.message.packet.cl); //remove host of message
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Client.list.put(message.packet.id, mst);
        resend.add(message.packet.id);
        if (invoketime == null) invoketime = mst.origtime.plusSeconds(3);
    }
    public void deleteclients(Vector<ClientData> clients){
        for (int i = 0; i < clients.size(); i++){
            Client.clients.remove(clients.elementAt(i));
            if (clients.elementAt(i).equals(Client.secroot)) {
                Client.connect(Client.thirdroot);
            }
        }
    }
}
