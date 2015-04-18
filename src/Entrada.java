import java.net.InetAddress;
import java.net.UnknownHostException;

public class Entrada {
    //TODO No se usa para no duplicar datos, ¿Se debería usar y pasar a un ArrayList?
    byte[] IPv4 = new byte[4];
    byte[] mascara = new byte[4];
    byte[] nextHoop= new byte[4];
    byte metrica;
    int timer;


    Entrada(String mascara,String nextHoop,int metrica){

        this(mascara,metrica);

        String[] nextHoop1 = nextHoop.split("\\.");
        for (int i = 0; i < 4; i++) {
            this.nextHoop[i] = (byte) (int) Integer.valueOf(nextHoop1[i]);
        }


    }

    Entrada(String mascara,int metrica){

        int mascara1 = 0xffffffff << (32 - Integer.valueOf(mascara));

        byte[] mascaraBytes = new byte[]{
                (byte)(mascara1 >>> 24), (byte)(mascara1 >> 16 & 0xff), (byte)(mascara1 >> 8 & 0xff), (byte)(mascara1 & 0xff) };

        InetAddress direccionMascara = null;
        try {
            direccionMascara = InetAddress.getByAddress(mascaraBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.mascara = direccionMascara.getAddress();

        System.out.println(metrica);
        this.metrica = (byte) metrica;
    }

    @Override
    public String toString() {
        InetAddress mask = null;
        InetAddress nextH = null;
        try {
            mask = InetAddress.getByAddress(mascara);
            nextH = InetAddress.getByAddress(nextHoop);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return mask + " NextHoop: " + nextH;

    }
}
