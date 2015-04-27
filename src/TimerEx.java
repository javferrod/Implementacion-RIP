import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class TimerEx {

    ArrayList<String> routers = new ArrayList<>();
    ArrayList<String> rutasConectadas = new ArrayList<>();

    static ArrayList<Entrada> tablaDirecciones = new ArrayList<>();

    static DatagramSocket socket;




    //TODO ¿Incluirlo en empezarMensajeOrdinario()?
    static TimerTask mensajeOrdinario = new TimerTask() {
        @Override
        public void run()
        {
            Paquete p = new Paquete(2, tablaDirecciones.size());
            for(Entrada e : tablaDirecciones)
                p.add(e.IPv4,e.mascara,e.nextHoop,e.metrica);

            try{
                socket.send(p.generarDatagramPacket());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    public void setPuerto(int puerto){
        try {
            socket = new DatagramSocket(puerto);
        } catch (SocketException e) {
            System.err.println("No se pudo acceder al puerto "+puerto);
        }
    }

    public void escucharPuerto(){
        //Nos preparamos para recibir una mensaje de 256 bytes
        byte[] mensaje_bytes = new byte[256];
        mensaje_bytes = new byte[256];
        //Creamos un contenedor de datagrama, el buffer será el array mensaje_bytes
        DatagramPacket paqueteRecibido = new DatagramPacket(mensaje_bytes,256);
        try {
            socket.receive(paqueteRecibido);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] p = paqueteRecibido.getData();


        if(p[0]==1){ //Es una pregunta
            if(p[4]==0 & p[23]==16){
                //Es una petición para enviar toda la tabla
            }
            else{
                Paquete pack = new Paquete(p);
                int i;
                for(Entrada e: pack.getEntradas()){
                    if(tablaDirecciones.contains(e));
                    //a[0] //comprobar si existe en la tabla
                    //a[1] //mascara
                    //

                }
            }

        }
        if(p[0]==2){ //Es una respuesta

        }
        //TODO condición de solo 1 paquete
        if(paqueteRecibido.getData()[0]==1 & paqueteRecibido.getData()[4]==0 & paqueteRecibido.getData()[23]==16){

        }
        if(paqueteRecibido.getData()[0]==1){
            //Es una solicitud, enviar la respuesta
        }
    }

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
                    tablaDirecciones.add(new Entrada(s[0], s[1], 1));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lanza un mensaje ordinario cada 30 segundos
     */
    public static void empezarMensajesOrdinarios(){
        Timer cada30segundos = new Timer();
        cada30segundos.schedule(mensajeOrdinario,10,3000);
    }

}