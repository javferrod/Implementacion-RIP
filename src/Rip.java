public class Rip {



    public static void main (String [ ] args) {
        if(args.length==0) Packet.genPassword("");
        else Packet.genPassword(args[0]);
        RipServer RIP = new RipServer();
        RIP.readConfig();
        RIP.setPort(7000);
        RIP.start();
    }





}
