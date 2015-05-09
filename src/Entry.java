import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Entry {
    byte[] IPv4 = new byte[4];
    byte[] mascara = new byte[4];
    byte[] nextHoop = new byte[4];
    byte metrica;
    long timer;
    boolean garbage;


    Entry(byte[] IPv4, byte[] mascara, byte metrica){
        this.IPv4=IPv4;
        this.mascara=mascara;
        this.metrica=metrica;
    }

    public void resetTimer(){
        timer = System.nanoTime();
    }
    public boolean isDirectConnected(){
        return Arrays.equals(nextHoop,new byte[4]); //TODO ¿isEmpty?
    }

    Entry(String IPv4, String mask, int metric){

        try {
            this.IPv4=InetAddress.getByName(IPv4).getAddress();
        } catch (UnknownHostException e) {
            System.err.println("IP no cumple el formato IPv4: "+ IPv4);
        }

        int mascara1 = 0xffffffff << (32 - Integer.valueOf(mask));

        byte[] mascaraBytes = new byte[]{
                (byte)(mascara1 >>> 24), (byte)(mascara1 >> 16 & 0xff), (byte)(mascara1 >> 8 & 0xff), (byte)(mascara1 & 0xff) };

        InetAddress MaksAddress = null;
        try {
            MaksAddress = InetAddress.getByAddress(mascaraBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        assert MaksAddress != null;
        this.mascara = MaksAddress.getAddress();

        this.metrica = (byte) metric;
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
        if(!(o instanceof Entry)) return false;

        Entry e = (Entry) o;
        return Arrays.equals(this.IPv4, e.IPv4) & Arrays.equals(this.mascara, e.mascara); //TODO ¿Como compararlo?
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(IPv4) + Arrays.hashCode(mascara);
    }
}
