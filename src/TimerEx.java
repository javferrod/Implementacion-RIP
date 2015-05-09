import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;

enum Tipo {
    RESPONSE(2), REQUEST(1);
    public final int v; // Variable interna donde almacenaremos la capacidad

    Tipo(int v) {
        this.v = v;
    }
}

class TimerEx {

    private static int MENSAJEORDINARIO = 30;
    private InetAddress IP;

    static MulticastSocket socket;
    private static Tabla tabla = new Tabla();
    private static ArrayList<InetAddress> routers = new ArrayList<>();

    public static void procesarPaquete(DatagramPacket paqueteRecibido) {

        System.out.println("INFO: Procesando paquete recibido");
        byte[] p = paqueteRecibido.getData();

        if (p[0] == Tipo.REQUEST.v) {
            //TODO ¿Correcta la comprobación de length?
            if (p.length == 24 & p[4] == 0 & p[23] == 16) {//Es una petición para enviar toda la tabla
                Paquete enviar = new Paquete(Tipo.RESPONSE, tabla.size());
                tabla.forEach(enviar::addEntrada);
                //TODO mejorar el envio del paquete, ya que se duplica el código
                try {
                    socket.send(enviar.generarDatagramPacket(paqueteRecibido.getAddress(), paqueteRecibido.getPort())); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else { //Es un request para algunas entradas
                Paquete recibido = new Paquete(p);
                int indexEntrada = 0;
                boolean hayEntradas = false;
                for (Entrada e : recibido.getEntradas()) {
                    Entrada eGuardada = tabla.get(e);
                    if(tabla.get(e)!=null){ //Si la tenemos
                        recibido.setMetrica(indexEntrada, eGuardada.metrica);
                        hayEntradas = true;
                    }else{ //Si no la tenemos
                        recibido.setMetrica(indexEntrada,(byte)16);
                    }
                    indexEntrada++;
                }
                //Enviamos el paquete de vuelta
                if (hayEntradas) recibido.setCommand(Tipo.RESPONSE);
                    enviar(recibido);
            }

        }
        if (p[0] == Tipo.RESPONSE.v) {
            Paquete recibido = new Paquete(p);
            /*
            Comprobar cabecera:
            -IPv4 origen perteneciente a una ruta conectada
            -Puerto de llegada por 520 (puerto RIP)
            -IPv4 que no sea la nuestra

             */
            for (Entrada e : recibido.getEntradas()) {
                System.out.println("INFO: Procesando entrada del paquete recibido: " + e);
                int metrica = e.metrica;
                int index = tabla.indexOf(e);
                /*--Comprobación de  entrada--*/
                //Comprobar IPv4 válida
                if (metrica < 1 || metrica > 16) continue;
                /*--FIN de comprobacion--*/
                metrica += 1;
                if (metrica > 16) metrica = 16;

                Entrada eVieja = tabla.get(e);




                if (eVieja==null) { //No existía la ruta
                    System.err.println("NO existe");
                    e.metrica = (byte) metrica;
                    e.nextHoop = paqueteRecibido.getAddress().getAddress();
                    //TODO flag cambiado
                    //TODO timeout
                    tabla.add(e);
                } else { //Existe la ruta
                    System.err.println("existe");
                    if (Arrays.equals(paqueteRecibido.getAddress().getAddress(), eVieja.nextHoop)) { //Viene del mismo router, por lo tanto es la misma ruta
                        /*TODO reset Timeout*/
                        if (metrica != eVieja.metrica) e.metrica = (byte) metrica;
                    } else {
                        if (metrica < eVieja.metrica) {
                            e.metrica = (byte) metrica;
                            e.nextHoop = paqueteRecibido.getAddress().getAddress();
                        }
                        if (metrica == eVieja.metrica) {
                            tabla.setwithHeuristic(e);
                            continue;
                        }
                    }
                    tabla.set(index, e); //Añadimos la entrada actualizada
                }
            }

        }
    }

    private Paquete mensajeOrdinario() {
        System.out.println(" [Mensaje ordinario] Enviando tabla de encaminamiento...");
        System.out.println("-------------------ESTADO DE LA TABLA-------------------");
        tabla.forEach(System.out::println);
        Paquete p = new Paquete(Tipo.RESPONSE, tabla.size());
        tabla.forEach(p::addEntrada);
        return p;
    }

    public void setPuerto(int puerto) {
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

    public void start() {
        Random r = new Random();
        ExecutorService ejecutor = Executors.newSingleThreadExecutor();
        Future<Integer> ign = ejecutor.submit(new escucharPuerto());
        while (true) {
            try {
                System.out.println("Escuchando...");
                ign.get(r.nextInt(5) + 30, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException ignored) {
            }
                enviar(mensajeOrdinario());

        }
    }

    public void leerConfiguracion() {


        /*Se buscan archivos con el formato ripconf-A.B.C.D.txt
        Se rechazarán aquellos que no tengan 4 bloques de números (IP)
        TODO ¿Rechazar aquellos que no tengan de 1 a 4 números por bloque?
        */

        File dir = new File("./");
        File[] archivosEncontrados = dir.listFiles((directorio, nombre) -> {
            return nombre.matches("ripconf-([0-9]+\\.){4}txt");
        });
        //Si no se encuentra ningún archivo, salir
        if (archivosEncontrados.length == 0) {
            System.err.println("No hay archivo de configuracion");
            System.err.println("Saliendo...");
            return;
        }
        System.out.println("Cargando archivo de configuración: " + archivosEncontrados[0].getName());
        try {
            IP = InetAddress.getByName(archivosEncontrados[0].getName().split("-|.txt")[1]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //Abrimos el archivo
        try (BufferedReader r = new BufferedReader(new FileReader(archivosEncontrados[0]))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                if (linea.matches("^([0-9]+\\.){3}[0-9]{1,4}$")) {
                    System.err.println("Añadiendo" +linea);
                    routers.add(InetAddress.getByName(linea)); //Corresponde a un router cercano
                }
                else if (linea.matches("^([0-9]+\\.){3}[0-9]{1,4}/[0-9]{1,2}$")) {
                    String[] s = linea.split("/");
                    tabla.add(new Entrada(s[0], s[1], 1)); //TODO metrica = 1 para ruta conectada¿?

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        for (InetAddress p : routers)
        System.out.println(p.getHostAddress());
    }

    public static void enviar(Paquete p){
        for (InetAddress ipDestino:routers){

            try {
                System.out.println(ipDestino);
                socket.send(p.generarDatagramPacket(ipDestino, 520)); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

class escucharPuerto implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        while (true) {
            DatagramPacket paqueteRecibido = new DatagramPacket(new byte[504], 504); //TODO Length del buffer
            TimerEx.socket.receive(paqueteRecibido);
            TimerEx.procesarPaquete(paqueteRecibido);
        }
    }
}
