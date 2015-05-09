import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class Table extends ArrayList<Entry>{

    LinkedBlockingQueue<Entry> TriggeredPackets;

    final static double TIMEOUT = 180*1000000000L;
    final static double GARBAGETIMEOUT=120*1000000000L;

    /**
     * Crea una instancia de Tabla a la vez que inicializa un Timer que se ejecutará cada 30 segundos.
     * Dicho Timer bloqueará el ArrayList buscando entradas expiradas o en cola para su eliminación.
     */
    Table(LinkedBlockingQueue<Entry> TriggeredPackets){
        super();
        this.TriggeredPackets = TriggeredPackets;
        Table table = this;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Comprobando tabla en busca de rutas expiradas");
                synchronized (table){
                    for (int i = 0; i < table.size(); i++) { //TODO cambiar este loop para que sea Entry e: table
                        double nano = System.nanoTime();
                        Entry e = table.get(i);
                        double segs =(nano - e.timer)/1000000000L;
                        System.err.println(segs);
                        //double nano = System.nanoTime();

                        if (e.isDirectConnected())
                            continue; //TODO ¿las rutas conectadas se eliminan? En este caso NO

                        if (!e.garbage & ((nano-e.timer > TIMEOUT)|e.metrica==(byte)16)) { //Marcando como basura cuando se cumple el tiempo
                            System.out.println("Marcando como basura: "+e);
                            e.garbage = true;
                            e.metrica = (byte) 16;
                            e.resetTimer();
                            table.set(e);
                        }
                        if(e.garbage & nano - e.timer > GARBAGETIMEOUT){ //Eliminamos la basura
                            System.out.println("Eliminando: "+e);
                            table.remove(e);
                        }
                    }
                }

            }
        }, 150*1000, 15*1000);
    }

    public Entry get(Entry e){
        synchronized (this) {
            int index = this.indexOf(e);
            if (index == -1) return null;
            return this.get(index);
        }
    }
    public boolean add(Entry e){
        e.resetTimer();
        return super.add(e);
    }
    public void set(Entry e){
        synchronized (this) {
            int index = this.indexOf(e);
            e.resetTimer();
            this.set(index, e);
            try {
                TriggeredPackets.put(e);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

        }
    }
    public void setwithHeuristic(Entry e){
        synchronized (this) {
            if((System.nanoTime()-this.get(e).timer)>TIMEOUT/2){ //Si la ruta antigua da signos de expirar, cambiamos a la nueva;
                int index = this.indexOf(e);
                e.resetTimer();
                this.set(index, e);
            }
        }
    }
}
