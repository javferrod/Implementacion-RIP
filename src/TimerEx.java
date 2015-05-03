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

    static MulticastSocket socket;
    private static ArrayList<Entrada> tablaDirecciones = new ArrayList<>();
    //Necesario para el Thread esucharPuerto
    private static ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private static Future<Integer> ign = ejecutor.submit(new escucharPuerto());
    ArrayList<String> routers = new ArrayList<>();
    private InetAddress IP;

    public static void procesarPaquete(DatagramPacket paqueteRecibido) {

        System.out.println("INFO: Procesando paquete recibido");
        byte[] p = paqueteRecibido.getData();

        if (p[0] == Tipo.REQUEST.v) {
            //TODO ¿Correcta la comprobación de length?
            if (p.length == 24 & p[4] == 0 & p[23] == 16) {//Es una petición para enviar toda la tabla
                Paquete enviar = new Paquete(Tipo.RESPONSE, tablaDirecciones.size());
                tablaDirecciones.forEach(enviar::addEntrada);
                //TODO mejorar el envio del paquete, ya que se duplica el código
                try {
                    socket.send(enviar.generarDatagramPacket(paqueteRecibido.getAddress(), paqueteRecibido.getPort())); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else { //Es un request para algunas entradas
                Paquete recibido = new Paquete(p);
                int indexEntrada = 0;
                boolean flag = false;
                for (Entrada e : recibido.getEntradas()) {
                    int i = tablaDirecciones.indexOf(e);
                    if (i != -1) {//Si la tenemos en la tabla, mandamos la metrica
                        recibido.setMetrica(indexEntrada, tablaDirecciones.get(i).metrica);
                        flag = true;
                    } else //¿No la tenemos? Metrica = 16
                        recibido.setMetrica(indexEntrada, (byte) 16);
                    indexEntrada++;
                }
                //Enviamos el paquete de vuelta
                if (flag) recibido.setCommand(Tipo.RESPONSE); //Si hay por lo menos una entrada, el paquete se envía
                try {
                    socket.send(recibido.generarDatagramPacket()); //TODO ¿A que dirección?
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                int index = tablaDirecciones.indexOf(e);
                /*--Comproción de  entrada--*/
                //Comprobar IPv4 válida
                if (metrica < 1 || metrica > 16) continue;
                /*--FIN de comporbacion--*/
                metrica += 1;
                if (metrica > 16) metrica = 16;

                if (index == -1) { //No existía la ruta
                    System.err.println("NO existe");
                    e.metrica = (byte) metrica;
                    e.nextHoop = paqueteRecibido.getAddress().getAddress();
                    //TODO flag cambiado
                    //TODO timeout
                    tablaDirecciones.add(e);
                } else { //Existe la ruta
                    System.err.println("existe");
                    Entrada eVieja = tablaDirecciones.get(index);
                    if (Arrays.equals(paqueteRecibido.getAddress().getAddress(), eVieja.nextHoop)) { //Viene del mismo router, por lo tanto es la misma ruta
                        /*TODO reset Timeout*/
                        if (metrica != eVieja.metrica) e.metrica = (byte) metrica;
                    } else {
                        if (metrica < eVieja.metrica) {
                            e.metrica = (byte) metrica;
                            e.nextHoop = paqueteRecibido.getAddress().getAddress();
                        }
                        if (metrica == eVieja.metrica) {
                            /*TODO Heuristica
                           If the new metric is the same as the old one, it is simplest to do
                           nothing further (beyond re-initializing the timeout, as specified
                           above); but, there is a heuristic which could be applied.  Normally,
                           it is senseless to replace a route if the new route has the same
                           metric as the existing route; this would cause the route to bounce
                           back and forth, which would generate an intolerable number of
                           triggered updates.  However, if the existing route is showing signs
                           of timing out, it may be better to switch to an equally-good
                           alternative route immediately, rather than waiting for the timeout to
                           happen.  Therefore, if the new metric is the same as the old one,
                           examine the timeout for the existing route.  If it is at least
                           halfway to the expiration point, switch to the new route.  This
                           heuristic is optional, but highly recommended.
                             */
                        }
                    }
                    tablaDirecciones.set(index, e); //Añadimos la entrada actualizada
                }
            }

        }
    }

    private DatagramPacket mensajeOrdinario() {
        System.out.println("[Mensaje ordinario] Enviando tabla de encaminamiento...");
        System.out.println("----------------ESTADO DE LA TABLA----------------");
        tablaDirecciones.forEach(System.out::println);
        Paquete p = new Paquete(Tipo.RESPONSE, tablaDirecciones.size());
        tablaDirecciones.forEach(p::addEntrada);
        return p.generarDatagramPacket();
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

        while (true) {
            try {
                System.out.println("Escuchando...");
                ign.get(r.nextInt(5) + 30, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException ignored) {
            }

            try {
                socket.send(mensajeOrdinario());
            } catch (IOException e) {
                e.printStackTrace();
            }

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
                    //tablaDirecciones.add(new Entrada(linea,"32",1)); //TODO ¿Máscara y metrica correcta para una ruta conectada?
                    //routers.add(linea); //Corresponde a un router cercano
                }
                if (linea.matches("^([0-9]+\\.){3}[0-9]{1,4}/[0-9]{1,2}$")) {
                    String[] s = linea.split("/");
                    tablaDirecciones.add(new Entrada(s[0], s[1], 1));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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