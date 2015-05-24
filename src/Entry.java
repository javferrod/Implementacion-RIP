import java.net.InetAddress;
import java.net.UnknownHostException;

public class Entry {
    private InetAddress IPv4;
    private InetAddress mascara;
    private InetAddress nextHop;
    private int metric;
    long timer;
    boolean garbage;


    Entry(byte[] IPv4, byte[] mascara, byte metric){
        try {
            this.IPv4=InetAddress.getByAddress(IPv4);
            this.mascara=InetAddress.getByAddress(mascara);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.metric =metric;
    }
    Entry(String IPv4, String mask, int metric){

        try {
            this.IPv4=InetAddress.getByName(IPv4);
        } catch (UnknownHostException e) {
            System.err.println("IP no cumple el formato IPv4: "+ IPv4);
        }

        int mascara1 = 0xffffffff << (32 - Integer.valueOf(mask));
        byte[] mascaraBytes = new byte[]{
                (byte)(mascara1 >>> 24), (byte)(mascara1 >> 16 & 0xff), (byte)(mascara1 >> 8 & 0xff), (byte)(mascara1 & 0xff) };

        try {
            mascara = InetAddress.getByAddress(mascaraBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }



        this.metric = metric;
    }
    public void resetTimer(){
        timer = System.nanoTime();
    }
    public byte[] getMask(){
        return mascara.getAddress();
    }
    public byte[] getIPv4(){
        return IPv4.getAddress();
    }
    public byte[] getNextHopBytes(){
        return nextHop.getAddress();
    }
    public InetAddress getNextHop(){
        return nextHop;
    }

    public byte getMetric(){
        return (byte) metric;
    }
    public void setMetric(int metric){
        this.metric = metric;
    }
    public void setNextHop(InetAddress nextHop){
        this.nextHop = nextHop;
    }

    public boolean isDirectConnected(){

        try {
            return nextHop.equals(InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException e) {
            return false;
        }
    }
    @Override
    public String toString() {
        return "IP: "+IPv4+" Máscara: "+mascara + " NextHop: " + nextHop +" Métrica: "+ metric;

    }

    @Override
    public boolean equals(Object o) {
        if(o == null)                return false;
        if(!(o instanceof Entry)) return false;

        Entry e = (Entry) o;
        return this.IPv4.equals(e.IPv4) & this.mascara.equals(e.mascara); //TODO ¿Como compararlo?
    }

    @Override
    public int hashCode() {
        return IPv4.hashCode() + mascara.hashCode();
    }
}
