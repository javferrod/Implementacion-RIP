import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Entrada {
    byte[] IPv4 = new byte[4];
    byte[] mascara = new byte[4];
    byte[] nextHoop= new byte[4];
    byte metrica;
    boolean cambiado;
    long timer;
    boolean garbage;


    Entrada(byte[] IPv4,byte[] mascara,byte metrica){
        this.IPv4=IPv4;
        this.mascara=mascara;
        this.metrica=metrica;
    }

    public void timerReset(){
        timer = System.nanoTime();
    }
    public boolean esRutaConectada(){
        return this.mascara== new byte[] {(byte)0,(byte)0,(byte)0,(byte)0}; //TODO ¿isEmpty?
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


        assert direccionMascara != null;
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
        return Arrays.hashCode(IPv4) + Arrays.hashCode(mascara);
    }
}
