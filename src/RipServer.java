import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;

enum Tipo {
    RESPONSE(2), REQUEST(1);
    public final int v; // Variable interna donde almacenaremos la capacidad

    Tipo(int v) {
        this.v = v;
    }
}

class RipServer {

    private InetAddress IP;

    static volatile DatagramSocket socket;


    static ArrayList<InetAddress> neighbors = new ArrayList<>();


    static LinkedBlockingQueue<Entry> TriggeredPackets = new LinkedBlockingQueue<>();
    static volatile Table entryTable = new Table(TriggeredPackets);


    static Thread receiverThread = new Thread(new Receiver(entryTable));
    Thread senderThread = new Thread(new Sender(entryTable,30,receiverThread));
    Thread triggeredUpdateThread = new Thread(new TriggeredSender(TriggeredPackets));


    public void start() {
        receiverThread.start();
        senderThread.start();
        triggeredUpdateThread.start();

        Console console = System.console();
        InetAddress IP = null,Mask = null;
        String input = console.readLine("Introduce IP para Triggered inmediato:");
        try {
            IP = InetAddress.getByName(input);
        } catch (UnknownHostException e) {
            System.err.println("IP erronea");
        }
        input = console.readLine("Introduce Mascara:");
        try {
            Mask = InetAddress.getByName(input);
        } catch (UnknownHostException e) {
            System.err.println("Mac erronea");
        }
        input = console.readLine("Introduce métrica");
        assert IP != null;
        assert Mask != null;
        Entry e = new Entry(IP.getAddress(),Mask.getAddress(),(byte)Integer.parseInt(input));
        try {
            entryTable.add(e);
            entryTable.TriggeredPackets.put(e);
        } catch (InterruptedException ignored) {
        }

    }

    public void setPort(int puerto) {
        try {
            socket = new DatagramSocket(puerto);
        } catch (SocketException e) {
            System.err.println("No se pudo acceder al puerto " + puerto);
        }
    }

    public static void sendUnicast(Packet p){
        receiverThread.interrupt();
        for (InetAddress iPDestination: neighbors){
            try {
                socket.send(p.getDatagramPacket(iPDestination, 520)); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }

    public void readConfig() {

        Enumeration<InetAddress> IPs = null;

        try {
            IPs = NetworkInterface.getByName("wlp2s0").getInetAddresses();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        assert IPs != null;
        IP = IPs.nextElement();
        while(!(IP instanceof Inet4Address))
            IP=IPs.nextElement();

        System.out.println("ripconf-"+IP.toString().substring(1,IP.toString().length())+".txt");

        File conf = new File("ripconf-"+IP.toString().substring(1,IP.toString().length())+".txt");
        //Abrimos el archivo
        try (BufferedReader r = new BufferedReader(new FileReader(conf))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                if (linea.matches("^([0-9]+\\.){3}[0-9]{1,4}$")) {
                    neighbors.add(InetAddress.getByName(linea)); //Corresponde a un router cercano
                }
                else if (linea.matches("^([0-9]+\\.){3}[0-9]{1,4}/[0-9]{1,2}$")) {
                    String[] s = linea.split("/");
                    entryTable.add(new Entry(s[0], s[1], 1));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static boolean isNeighbor(InetAddress IP){
        for (InetAddress address : neighbors){
            if(address.equals(IP))
                return true;
        }
        return false;
    }




}


