import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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


    LinkedBlockingQueue<Entry> TriggeredPackets = new LinkedBlockingQueue<>();
    Table entryTable = new Table(TriggeredPackets);




    Thread receiverThread = new Thread(new Receiver(entryTable));
    Thread senderThread = new Thread(new Sender(entryTable,30,receiverThread));
    Thread triggeredUpdateThread = new Thread(new TriggeredSender(TriggeredPackets));


    public void start() {
        receiverThread.start();
        senderThread.start();
        triggeredUpdateThread.start();
    }

    public void setPort(int puerto) {
        try {
            socket = new DatagramSocket(puerto);
        } catch (SocketException e) {
            System.err.println("No se pudo acceder al puerto " + puerto);
        }
    }

    public static void sendUnicast(Packet p){
        for (InetAddress iPDestination: neighbors){
            try {
                socket.send(p.getDatagramPacket(iPDestination, 7000)); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
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

        System.out.println(IP);
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




}


