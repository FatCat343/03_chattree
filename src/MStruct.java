import java.sql.Time;
import java.time.LocalTime;
import java.util.Vector;

public class MStruct {
    //message, sendtime, origtime, list <id, clientIP>
    public Message message;
    public LocalTime sendtime; //time of last sending
    public LocalTime origtime; //time of first sending
    public Vector<ClientData> branches = new Vector<ClientData>(); //clients where to send

}
