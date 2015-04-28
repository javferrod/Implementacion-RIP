import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Rip {



    public static void main (String [ ] args) {

    }

    public static void leerConfiguracion (){

        ArrayList<String> routers = new ArrayList<>();
        ArrayList<String> rutasConectadas = new ArrayList<>();
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
            System.out.println("WARNING: No hay archivo de configuracion");
            return;
        }

        //Abrimos el archivo
        try (BufferedReader r = new BufferedReader(new FileReader(archivosEncontrados[0]))) {
            String linea;
            while ( (linea = r.readLine())!= null) {
                if(linea.matches("^([0-9]+\\.){3}[0-9]{1,4}$")){
                    routers.add(linea); //Corresponde a un router cercano
                }
                if(linea.matches("^([0-9]+\\.){3}[0-9]{1,4}/[0-9]{1,2}$")){
                    rutasConectadas.add(linea); //Corresponde a una ruta conectada
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(routers);
        System.out.println(rutasConectadas);
    }


    /**
     * Cada 30s
     */


}
