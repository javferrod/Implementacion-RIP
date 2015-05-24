import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Packet {
    private ByteBuffer content;
    private int index;
    Packet(Tipo t, int numEntradas){
        content = ByteBuffer.allocate(4+20*numEntradas);
       /*-----HEADER----*/
        content.put(0, (byte) t.v);                      //Command
        content.put(1, (byte) 2);                        //Version de RIP
        content.put(2, (byte) 0);                        //Relleno
        content.put(3, (byte) 0);                        //Relleno
    }
    Packet(byte[] mensaje){
        if((mensaje.length-4)%20!=0) return; //TODO como transmitirselo a la clase que llama? !=null?
        this.content =ByteBuffer.wrap(mensaje);
    }



    ArrayList<Entry> getEntrys(){
        ArrayList<Entry> RIPentries = new ArrayList<>();
        for (int j = 0; j < (content.limit()-4)/20; j++) {
            if(content.get(j * 20 + 8)==(byte)0) return RIPentries; //Cuando no haya IPv4 significa que ya no hay más entradas
            RIPentries.add(new Entry(
                    new byte[]{content.get(j * 20 + 8), content.get(j * 20 + 9), content.get(j * 20 + 10), content.get(j * 20 + 11)}, //Añade la IPv4
                    new byte[]{content.get(j * 20 + 12), content.get(j * 20 + 13), content.get(j * 20 + 14), content.get(j * 20 + 15)}, //Añade la mascara
                    content.get(j * 20 + 23)));  //Añade la metrica)
        }
        return RIPentries;
    }

    void setMetric(int index, byte metric){
        content.put(20 * (index - 1) + 23, metric);
    }
    void setCommand(Tipo t){
        content.put(0, (byte) t.v);
    }
    void addEntry(Entry e){
        /*--Entry table--*/
        content.put(4 + index * 20, (byte) 0);        //Address family
        content.put(5 + index * 20, (byte) 2);        //TODO http://tools.ietf.org/html/rfc1058 dice que 2 (¿?)
        content.put(6 + index * 20, (byte) 0);        //Route tag
        content.put(7 + index * 20, (byte) 0);
        content.put(8 + index * 20, e.IPv4[0]);        //IPv4 address
        content.put(9 + index * 20, e.IPv4[1]);
        content.put(10 + index * 20, e.IPv4[2]);
        content.put(11 + index * 20, e.IPv4[3]);
        content.put(12 + index * 20, e.mascara[0]);       //Subnet mask
        content.put(13 + index * 20, e.mascara[1]);
        content.put(14 + index * 20, e.mascara[2]);
        content.put(15 + index * 20, e.mascara[3]);
        content.put(16 + index * 20, e.nextHoop[0]);   //Next hop
        content.put(17 + index * 20, e.nextHoop[1]);
        content.put(18 + index * 20, e.nextHoop[2]);
        content.put(19 + index * 20, e.nextHoop[3]);
        content.put(20 + index * 20, (byte) 0);       //Metric
        content.put(21 + index * 20, (byte) 0);
        content.put(22 + index * 20, (byte) 0);
        content.put(23 + index * 20, e.metrica);
        index++;

    }
    DatagramPacket getDatagramPacket(InetAddress addrDestino, int puertoDestino){

        //Split Horizon with Poison Reverse
        ByteBuffer tContent = content;
        byte [] nextHoop;
        for (int j = 0; j < index; j++) {
            nextHoop= new byte[]{tContent.get(16 + j * 20) , tContent.get(17 + j * 20),
                    tContent.get(18 + j * 20), tContent.get(19 + j * 20)};
            InetAddress nextHoopa = null;
            try {
                 nextHoopa = InetAddress.getByAddress(nextHoop);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            assert nextHoopa != null;
            if(nextHoopa.equals(addrDestino))
                tContent.put(23+20*j,(byte)16);
        }
        return new DatagramPacket(tContent.array(),tContent.limit(), addrDestino, puertoDestino);
    }



}
