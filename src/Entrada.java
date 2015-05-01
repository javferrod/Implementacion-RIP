import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Entrada {
    //TODO No se usa para no duplicar datos, ¿Se debería usar y pasar a un ArrayList?
    byte[] IPv4 = new byte[4];
    byte[] mascara = new byte[4];
    byte[] nextHoop= new byte[4];
    byte metrica;

    Entrada(byte[] IPv4,byte[] mascara,byte metrica){
        this.IPv4=IPv4;
        this.mascara=mascara;
        this.metrica=metrica;
    }
    Entrada(String IPv4, String mascara,String nextHoop,int metrica){
        this(IPv4,mascara, metrica);

        String[] nextHoop1 = nextHoop.split("\\.");
        for (int i = 0; i < 4; i++) {
            this.nextHoop[i] = (byte) (int) Integer.valueOf(nextHoop1[i]);
        }


    }
    Entrada(String IPv4, String mascara,int metrica){

        try {
            this.IPv4=InetAddress.getByName(IPv4).getAddress();
        } catch (UnknownHostException e) {
            System.err.println("IP no cumple el formato IPv4: "+ IPv4);
        }

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

        this.metrica = (byte) metrica;
    }

    @Override
    public String toString() {
        InetAddress IP = null;
        InetAddress mask = null;
        InetAddress nextH = null;
        try {
            IP = InetAddress.getByAddress(this.IPv4);
            mask = InetAddress.getByAddress(this.mascara);
            if(this.nextHoop!=null)
            nextH = InetAddress.getByAddress(this.nextHoop);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "IP: "+IP+" Máscara: "+mask + " NextHoop: " + nextH+" Métrica: "+this.metrica;

    }

    @Override
    public boolean equals(Object o) {
        if(o == null)                return false;
        if(!(o instanceof Entrada)) return false;

        Entrada e = (Entrada) o;
        return Arrays.equals(this.IPv4, e.IPv4) & Arrays.equals(this.mascara, e.mascara); //TODO ¿Como compararlo?
    }

    @Override
    public int hashCode() {
        return IPv4.hashCode()+mascara.hashCode();
    }
}
