public class Rip {



    public static void main (String [ ] args) {

        RipServer RIP = new RipServer();
        RIP.readConfig();
        RIP.setPort(520);
        RIP.start();


    }





}
