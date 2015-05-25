import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Clase creada para hacer más legible el código
 */
enum Tipo {
    RESPONSE(2), REQUEST(1);
    public final int v; // Almacenamos el valor del campo para REQUEST o RESPONSE

    Tipo(int v) {
        this.v = v;
    }
}

/**
 * Aporta métodos estáticos que emplearán el resto de clases, también
 * contiene los datos e inicializa los distintos procesos
 */
class RipServer {


    static volatile DatagramSocket socket;


    static ArrayList<InetAddress> neighbors = new ArrayList<>();


    static LinkedBlockingQueue<Entry> TriggeredPackets = new LinkedBlockingQueue<>();
    static volatile Table entryTable = new Table(TriggeredPackets);

    static Thread receiverThread = new Thread(new Receiver(entryTable));
    Thread senderThread = new Thread(new Sender(entryTable,30,receiverThread));
    Thread triggeredUpdateThread = new Thread(new TriggeredSender(TriggeredPackets));


    /**
     * Inicializa los distintos procesos
     */
    public void start() {
        receiverThread.start();
        senderThread.start();
        triggeredUpdateThread.start();
    }

    /**
     * Inicia el servidor en el puerto indicado
     * @param puerto
     */
    public void setPort(int puerto) {
        try {
            socket = new DatagramSocket(puerto);
        } catch (SocketException e) {
            System.err.println("No se pudo acceder al puerto " + puerto);
        }
    }

    /**
     * Envía un paquete a los routers directamente conectados
     * @param p
     */
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

    /**
     * Obtiene la IP de la interfaz eth0, lee el archivo de
     * configuración y lo vuelca en la tabla de entradas
     */
    public void readConfig() {

        Enumeration<InetAddress> IPs = null;
        String IPstring;
        InetAddress IP;

        try {
            IPs = NetworkInterface.getByName("wlp2s0").getInetAddresses();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        assert IPs != null;
        IP = IPs.nextElement();
        while(!(IP instanceof Inet4Address))
            IP=IPs.nextElement();

        IPstring = IP.toString().substring(1,IP.toString().length());
        System.out.println("ripconf-" + IPstring + ".txt");
        entryTable.add(new Entry(IPstring, "32", 0)); //Añadimos nuestra propia IP a la tabla

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
                    entryTable.add(new Entry(s[0], s[1], 1)); //Corresponde a una ruta conectada

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Comprueba si {@param IP} corresponde a un vecino válido
     * en caso contrario devuelve false
     * @param IP
     * @return
     */
    static boolean isNeighbor(InetAddress IP){
        for (InetAddress address : neighbors){
            if(address.equals(IP))
                return true;
        }
        return false;
    }




}


