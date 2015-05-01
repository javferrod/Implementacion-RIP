import java.io.IOException;

public class Rip {



    public static void main (String [ ] args) {

        TimerEx database = new TimerEx();
        database.leerConfiguracion();
        database.setPuerto(7000);
        try {
            database.escucharPuerto();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}
