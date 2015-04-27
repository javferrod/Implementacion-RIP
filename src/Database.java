import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

@Deprecated
public class Database {


    ArrayList<String> routers = new ArrayList<>();
    ArrayList<String> rutasConectadas = new ArrayList<>();
    HashMap<InetAddress,Entrada> tablaDirecciones = new HashMap<>();
    int numentradas = 1; //Numero de entradas a transmitir en el mensaje RIP. De momento de manera manual

    public void leerConfiguracion (){


        /*Se buscan archivos con el formato ripconf-A.B.C.D.txt
        Se rechazarán aquellos que no tengan 4 bloques de números (IP)
        TODO ¿Rechazar aquellos que no tengan de 1 a 4 números por bloque?
        */
        File dir = new File("src");
        File[] archivosEncontrados = dir.listFiles((directorio, nombre) -> {
            return nombre.matches("ripconf-([0-9]+\\.){4}txt");
        });

        //Si no se encuentra ningún archivo, salir
        if(archivosEncontrados.length==0){
            System.err.println("No hay archivo de configuracion");
            System.err.println("Saliendo...");
            return;
        }

        //Abrimos el archivo
        try (BufferedReader r = new BufferedReader(new FileReader(archivosEncontrados[0]))) {
            String linea;
            while ( (linea = r.readLine())!= null) {
                if(linea.matches("^([0-9]+\\.){3}[0-9]{1,4}$")){
                    routers.add(linea); //Corresponde a un router cercano
                }
                if(linea.matches("^([0-9]+\\.){3}[0-9]{1,4}\\/[0-9]{1,2}$")){
                    String[] s = linea.split("/");
                    //tablaDirecciones.put(InetAddress.getByName(s[0]),new Entrada(s[1],1));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    final public void mensajeOrdinario() throws IOException {


        DatagramSocket socket = new DatagramSocket();
        socket.connect(new InetSocketAddress(520));
    }

    /**
     * Genera un paquete RIP de respuesta a partir de los parametros pasados
     * @param IPv4 Array con IPs en formato A.B.C.D/len
     * @param metric Array con las metrica de la entrada
     * @param nextHoop IPs siguiente salto con formato A.B.C.D, pueden ser String vacios
     * @return DatagramPacket con la información pasada
     */
    public DatagramPacket generarPaquete(byte[] IPv4, byte[] mascara,byte metric[], byte[] nextHoop){

        ByteBuffer mensaje = ByteBuffer.allocate(4+20*IPv4.length);

        /*-----HEADER----*/
        mensaje.put(0,(byte)2);                        //Command = 2 (response)
        mensaje.put(1,(byte)2);                        //Version de RIP
        mensaje.put(2,(byte)0);                        //Relleno
        mensaje.put(3,(byte)0);                        //Relleno

        for (int i = 0; i < IPv4.length ; i++) {
            /*--Entry table--*/
            mensaje.put(4+i*20,(byte)0);        //Address family
            mensaje.put(5+i*20,(byte)2);        //TODO http://tools.ietf.org/html/rfc1058 dice que 2 (¿?)
            mensaje.put(6+i*20,(byte)0);        //Route tag
            mensaje.put(7+i*20,(byte)0);
            mensaje.put(8+i*20,IPv4[0]);        //IPv4 address
            mensaje.put(9+i*20,IPv4[1]);
            mensaje.put(10+i*20,IPv4[2]);
            mensaje.put(11+i*20,IPv4[3]);
            mensaje.put(12+i*20,(byte)0);       //Subnet mask
            mensaje.put(13+i*20,(byte)0);
            mensaje.put(14+i*20,(byte)0);
            mensaje.put(15+i*20,mascara[i]);
            mensaje.put(16+i*20,nextHoop[0]);   //Next hop
            mensaje.put(17+i*20,nextHoop[1]);
            mensaje.put(18+i*20,nextHoop[2]);
            mensaje.put(19+i*20,nextHoop[3]);
            mensaje.put(20+i*20,(byte)0);       //Metric
            mensaje.put(21+i*20,(byte)0);
            mensaje.put(22+i*20,(byte)0);
            mensaje.put(23+i*20,metric[i]);
        }

        /*-----HEADER-----
        mensaje[0]=2; //Command = 2 (response)
        mensaje[1]=2; //Version de RIP
        mensaje[3]=0; //Relleno
        mensaje[2]=0; //Relleno
        */
        /*--Entry table--
        mensaje[3]=0; //Address family
        mensaje[4]=2; //TODO http://tools.ietf.org/html/rfc1058 dice que 2 (¿?)
        mensaje[5]=0; //Route tag
        mensaje[6]=0;
        mensaje[7]=(byte)Integer.parseInt(IP[0]); //IPv4 address
        mensaje[8]=(byte)Integer.parseInt(IP[1]);
        mensaje[9]=(byte)Integer.parseInt(IP[2]);
        mensaje[10]=(byte)Integer.parseInt(IP[3]);
        mensaje[11]=0; //Subnet mask
        mensaje[12]=0;
        mensaje[13]=0;
        mensaje[14]=(byte)mascara;
        mensaje[15]=(byte)nextH[0]; //Next hop
        mensaje[16]=(byte)nextH[1];
        mensaje[17]=(byte)nextH[2];
        mensaje[18]=(byte)nextH[3];
        mensaje[19]=0; //Metric
        mensaje[20]=0;
        mensaje[21]=0;
        mensaje[22]=Metric;
*/
        return new DatagramPacket(mensaje.array(), 4+20*numentradas);
    }




}
