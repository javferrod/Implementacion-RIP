import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
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
                p.addEntrada(e);
            try{
                socket.send(p.generarDatagramPacket());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };



    public DatagramPacket mensajeOrdinario(){
        Paquete p = new Paquete(2, tablaDirecciones.size());
        for(Entrada e : tablaDirecciones)
            p.addEntrada(e);
        return p.generarDatagramPacket();
    }

    public void setPuerto(int puerto){
        try {
            socket = new DatagramSocket(puerto);
        } catch (SocketException e) {
            System.err.println("No se pudo acceder al puerto "+puerto);
        }
    }

    public void escucharPuerto(int puerto) throws IOException {
        MulticastSocket socket = new MulticastSocket(puerto);
        socket.joinGroup(InetAddress.getByName("224.0.0.9."));
        //Escuchamos el puerto hasta que se cumpla el timeout (30s)
        //TODO NO se deben utilizar excepciones para controlar el flujo normal de un programa, ¿cambiar esto?
        while(true) {
            assert socket != null;
            socket.setSoTimeout(3000);
            DatagramPacket paqueteRecibido = new DatagramPacket(new byte[256], 256); //TODO Length del buffer
            try {
                socket.receive(paqueteRecibido);
            } catch (SocketTimeoutException e) {
                socket.send(mensajeOrdinario());
                continue;
            }
            procesarPaquete(paqueteRecibido);
        }
    }

    public void procesarPaquete(DatagramPacket paqueteRecibido){





        //Nos preparamos para recibir una mensaje de 256 bytes
        //byte[] mensaje_bytes = new byte[256];
        //mensaje_bytes = new byte[256];
        //Creamos un contenedor de datagrama, el buffer será el array mensaje_bytes
        //DatagramPacket paqueteRecibido = new DatagramPacket(mensaje_bytes,256);

        byte[] p = paqueteRecibido.getData();

        if(p[0]==1){ //Es una pregunta
            //TODO ¿Correcta la comprobación de length?
            if(p.length==24&p[4]==0 & p[23]==16){//Es una petición para enviar toda la tabla
                Paquete enviar = new Paquete(2, tablaDirecciones.size());
                for(Entrada e : tablaDirecciones)
                    enviar.addEntrada(e);
               //TODO mejorar el envio del paquete, ya que se duplica el código
                try {
                    socket.send(enviar.generarDatagramPacket(paqueteRecibido.getAddress(),paqueteRecibido.getPort())); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{ //Es un request para algunas entradas
                Paquete recibido = new Paquete(p);
                int indexEntrada=0;
                boolean flag = false;
                for(Entrada e: recibido.getEntradas()){
                    int i=tablaDirecciones.indexOf(e);
                    if(i!=-1) {//Si la tenemos en la tabla, le mandamos la metrica
                        recibido.setMetrica(indexEntrada, tablaDirecciones.get(i).metrica);
                        flag = true;
                    }
                    else //¿No la tenemos? Metrica = 16
                        recibido.setMetrica(indexEntrada,(byte)16);
                }
                //Enviamos el paquete de vuelta
                if(flag) recibido.setCommand(2); //Si hay por lo menos una entrada, el paquete se envía
                try {
                    socket.send(recibido.generarDatagramPacket()); //TODO ¿A que dirección?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        if(p[0]==2){ //Es una respuesta

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