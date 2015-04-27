import java.net.InetAddress;
import java.net.UnknownHostException;

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

    @Override
    public boolean equals(Object o) {
        if(o == null)                return false;
        if(!(o instanceof Entrada)) return false;

        Entrada e = (Entrada) o;
        if(e.IPv4.equals(e.IPv4) & e.mascara.equals(mascara))
            return true; //TODO mascara debe de comprobarse si es menor, porque entonces si la contiene ¿No?
        return false;
    }
}
