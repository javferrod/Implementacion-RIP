import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Receiver implements Runnable {
    
    Table entryTable;
    
    Receiver(Table entryTable){
        this.entryTable=entryTable;
    }

    @Override
    public void run() {
        while (true) {
            DatagramPacket paqueteRecibido = new DatagramPacket(new byte[504], 504); //TODO Length del buffer
            try {
                RipServer.socket.receive(paqueteRecibido);
            } catch (IOException e) {
                e.printStackTrace();
            }
            procesarPaquete(paqueteRecibido);
        }
    }

    public void procesarPaquete(DatagramPacket paqueteRecibido) {
        byte[] p = paqueteRecibido.getData();

        if (p[0] == Tipo.REQUEST.v) {
            //TODO ¿Correcta la comprobación de length?
            if (p.length == 24 & p[4] == 0 & p[23] == 16) {//Es una petición para enviar toda la TimerEx.tabla
                Packet enviar = new Packet(Tipo.RESPONSE, entryTable.size());
                entryTable.forEach(enviar::addEntry);
                try {
                    RipServer.socket.send(enviar.getDatagramPacket(paqueteRecibido.getAddress(), paqueteRecibido.getPort())); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else { //Es un request para algunas entradas
                Packet pReceivec = new Packet(p);
                int entryIndex = 0;
                boolean hayEntradas = false;
                for (Entry e : pReceivec.getEntrys()) {
                    Entry ourEntry = entryTable.get(e);
                    if(entryTable.get(e)!=null){ //Si la tenemos
                        pReceivec.setMetric(entryIndex, ourEntry.metrica);
                        hayEntradas = true;
                    }else{ //Si no la tenemos
                        pReceivec.setMetric(entryIndex, (byte) 16);
                    }
                    entryIndex++;
                }
                //Enviamos el paquete de vuelta
                if (hayEntradas) pReceivec.setCommand(Tipo.RESPONSE);
                //TODO enviar solo al que respondió
            }

        }
        if (p[0] == Tipo.RESPONSE.v) {
            Packet recibido = new Packet(p);
            /*
            Comprobar cabecera:
            -IPv4 origen perteneciente a una ruta conectada
            -Puerto de llegada por 520 (puerto RIP)
            -IPv4 que no sea la nuestra

             */
            for (Entry newEntry : recibido.getEntrys()) {
                System.out.println("INFO: Procesando entrada del paquete recibido: " + newEntry);
                int metrica = newEntry.metrica;
                /*--Comprobación de  entrada--*/
                //Comprobar IPv4 válida
                if (metrica < 0 || metrica > 16) continue;
                /*--FIN de comprobacion--*/
                metrica += 1;
                if (metrica > 16) metrica = 16;
                Entry old = entryTable.get(newEntry);

                if (old==null) { //No existía la ruta
                    System.err.println("NO existe");
                    newEntry.metrica = (byte) metrica;
                    newEntry.nextHoop = paqueteRecibido.getAddress().getAddress();
                    entryTable.add(newEntry);
                } else { //Existe la ruta
                    System.err.println("existe");
                    InetAddress nextHoopa = null;
                    try {
                        nextHoopa = InetAddress.getByAddress(old.nextHoop);
                    } catch (UnknownHostException e1) {
                        e1.printStackTrace();
                    }
                    if (paqueteRecibido.getAddress().equals(nextHoopa)) { //Viene del mismo router, por lo tanto es la misma ruta
                        if(old.metrica == (byte) metrica){
                            entryTable.refresh(old); //Solo reiniciamos el timeOut
                            continue;
                        }
                        old.metrica = (byte) metrica;
                        entryTable.set(old);


                    }
                    else {
                        if (metrica < old.metrica) {
                            newEntry.metrica = (byte) metrica;
                            newEntry.nextHoop = paqueteRecibido.getAddress().getAddress();
                            entryTable.set(newEntry); //Añadimos la entrada actualizada
                        }
                        if (metrica == old.metrica) {
                            entryTable.setwithHeuristic(newEntry);
                        }
                    }
                }
            }

        }

    }



}
