import java.io.IOException;
import java.net.DatagramPacket;

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

    public void procesarPaquete(DatagramPacket receivedPacket) {

        if(receivedPacket.getPort()!=520) return;
        if(RipServer.isNeighbor(receivedPacket.getAddress())) return;

        byte[] p = receivedPacket.getData();

        if(!(p[0]==Tipo.REQUEST.v|p[0]==Tipo.RESPONSE.v)) return;
        if(p[1]!=(byte)2) return;





        if (p[0] == Tipo.REQUEST.v) {
            //TODO ¿Correcta la comprobación de length?
            if (p.length == 24 & p[4] == 0 & p[23] == 16) {//Es una petición para enviar toda la TimerEx.tabla
                Packet enviar = new Packet(Tipo.RESPONSE, entryTable.size());
                entryTable.forEach(enviar::addEntry);
                try {
                    RipServer.socket.send(enviar.getDatagramPacket(receivedPacket.getAddress(), receivedPacket.getPort())); //TODO ¿.getPort() es el puerto de origen del paquete o el puerto destino?
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
                        pReceivec.setMetric(entryIndex, ourEntry.getMetric());
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
                int metrica = newEntry.getMetric();



                /*--Comprobación de  entrada--*/
                //Comprobar IPv4 válida
                if (metrica < 0 || metrica > 16) continue;
                /*--FIN de comprobacion--*/
                metrica += 1;
                if (metrica > 16) metrica = 16;
                Entry old = entryTable.get(newEntry);

                if (old==null) { //No existía la ruta
                    System.err.println("NO existe");
                    newEntry.setMetric(metrica);
                    newEntry.setNextHop(receivedPacket.getAddress());
                    entryTable.add(newEntry);
                } else { //Existe la ruta
                    System.err.println("existe");
                    if (receivedPacket.getAddress().equals(old.getNextHop())) { //Viene del mismo router, por lo tanto es la misma ruta
                        if(old.getMetric() == (byte) metrica){
                            entryTable.refresh(old); //Solo reiniciamos el timeOut
                            continue;
                        }
                        old.setMetric(metrica);
                        entryTable.set(old);


                    }
                    else {
                        if (metrica < old.getMetric()) {
                            newEntry.setMetric(metrica);
                            newEntry.setNextHop(receivedPacket.getAddress());
                            entryTable.set(newEntry); //Añadimos la entrada actualizada
                        }
                        if (metrica == old.getMetric()) {
                            entryTable.setwithHeuristic(newEntry);
                        }
                    }
                }
            }

        }

    }



}
