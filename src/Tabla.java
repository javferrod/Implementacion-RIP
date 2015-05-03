import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Tabla extends ArrayList<Entrada>{

    final static double TIMEOUT = 180*1000000000L;
    final static double GARBAGETIMEOUT=120*1000000000L;

    /**
     * Crea una instancia de Tabla a la vez que inicializa un Timer que se ejecutará cada 30 segundos.
     * Dicho Timer bloqueará el ArrayList buscando entradas expiradas o en cola para su eliminación.
     */
    Tabla(){
        super();
        Tabla tabla = this;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Comprobando tabla en busca de rutas expiradas");
                synchronized (tabla){
                    for (int i = 0; i < tabla.size(); i++) {
                        double nano = System.nanoTime();
                        Entrada e = tabla.get(i);
                        double segs =(nano - e.timer)/1000000000L;
                        System.err.println(segs);
                        //double nano = System.nanoTime();

                        if (e.esRutaConectada())
                            continue; //TODO ¿las rutas conectadas se eliminan? En este caso NO

                        if (!e.garbage & ((nano-e.timer > TIMEOUT)|e.metrica==(byte)16)) { //Marcando como basura cuando se cumple el tiempo
                            System.out.println("Marcando como basura: "+e);
                            e.garbage = true;
                            e.metrica = (byte) 16;
                            e.timerReset();
                            tabla.set(i,e);
                        }
                        if(e.garbage & nano - e.timer > GARBAGETIMEOUT){ //Eliminamos la basura
                            System.out.println("Eliminando: "+e);
                            tabla.remove(e);
                        }
                    }
                }

            }
        }, 30*1000, 30*1000);
    }

    public Entrada get(Entrada e){
        synchronized (this) {
            int index = this.indexOf(e);
            if (index == -1) return null;
            //if(super.get(index).timer+)
            return this.get(index);
        }
    }
    public boolean add(Entrada e){
        e.timerReset();
        return super.add(e);
    }
    public void set(Entrada e){
        synchronized (this) {
            int index = this.indexOf(e);
            e.cambiado = true;
            e.timerReset();
            this.set(index, e);
        }
    }
    public void setwithHeuristic(Entrada e){
        synchronized (this) {
            if((System.nanoTime()-this.get(e).timer)>TIMEOUT/2){
                this.set(e); //Si la ruta antigua da signos de expirar, cambiamos a la nueva;
            }
        }
    }
}
