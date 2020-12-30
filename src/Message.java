class MType{
    public static int message = 0;
    public static int reply = 1;
    public static int secroot = 2;
    public static int secroot_self = 4;
    public static int check = 3;
    public static int single = 6;
    public static int broadcast = 7;
}
public class Message {
    public msg packet; //Datagramm packet
    public int type;
    public String host;
}
