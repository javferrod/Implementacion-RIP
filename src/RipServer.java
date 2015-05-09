import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

enum Tipo {
    RESPONSE(2), REQUEST(1);
    public final int v; // Variable interna donde almacenaremos la capacidad

    Tipo(int v) {
        this.v = v;
    }
}

class RipServer {

    private static int MENSAJEORDINARIO = 30;
    private InetAddress IP;

    static volatile MulticastSocket socket;


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
            socket = new MulticastSocket(puerto);
            socket.joinGroup(InetAddress.getByName("224.0.0.9"));
            socket.setLoopbackMode(true);
        } catch (SocketException e) {
            System.err.println("No se pudo acceder al puerto " + puerto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendUnicast(Packet p){
        for (InetAddress iPDestination: neighbors){
            try {
                socket.send(p.getDatagramPacket(iPDestination, 520)); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void readConfig() {


        /*Se buscan archivos con el formato ripconf-A.B.C.D.txt
        Se rechazarán aquellos que no tengan 4 bloques de números (IP)
        TODO ¿Rechazar aquellos que no tengan de 1 a 4 números por bloque?
        */

        File dir = new File("./");
        File[] foundFiles = dir.listFiles((directorio, nombre) -> {
            return nombre.matches("ripconf-([0-9]+\\.){4}txt");
        });
        //Si no se encuentra ningún archivo, salir
        if (foundFiles.length == 0) {
            System.err.println("No hay archivo de configuracion");
            System.err.println("Saliendo...");
            return;
        }
        System.out.println("Cargando archivo de configuración: " + foundFiles[0].getName());
        try {
            IP = InetAddress.getByName(foundFiles[0].getName().split("-|.txt")[1]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //Abrimos el archivo
        try (BufferedReader r = new BufferedReader(new FileReader(foundFiles[0]))) {
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


