import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Paquete {
    private ByteBuffer mensaje;
    private int i;
    Paquete(Tipo t,int numEntradas){
        mensaje = ByteBuffer.allocate(4+20*numEntradas);
       /*-----HEADER----*/
        mensaje.put(0, (byte) t.v);                      //Command = 2 (response)
        mensaje.put(1, (byte) 2);                        //Version de RIP
        mensaje.put(2, (byte) 0);                        //Relleno
        mensaje.put(3, (byte) 0);                        //Relleno
    }
    Paquete(byte[] mensaje){
        if((mensaje.length-4)%20!=0) return; //TODO como transmitirselo a la clase que llaam? !=null?
        this.mensaje=ByteBuffer.wrap(mensaje);
    }



    ArrayList<Entrada> getEntradas(){
        ArrayList<Entrada> entradas= new ArrayList<>();
        for (int j = 0; j < (mensaje.limit()-4)/20; j++) {
            if(mensaje.get(j * 20 + 8)==(byte)0) return entradas; //Cuando no haya IPv4 significa que ya no hay más entradas
            entradas.add(new Entrada(
                    new byte[]{mensaje.get(j * 20 + 8), mensaje.get(j * 20 + 9), mensaje.get(j * 20 + 10), mensaje.get(j * 20 + 11)}, //Añade la IPv4
                    new byte[]{mensaje.get(j * 20 + 12), mensaje.get(j * 20 + 13), mensaje.get(j * 20 + 14), mensaje.get(j * 20 + 15)}, //Añade la mascara
                    mensaje.get(j * 20 + 23)));  //Añade la metrica)
        }
        return entradas;
    }

    void setMetrica(int index, byte metrica){
        mensaje.put(20*(index-1)+23,metrica);
    }
    void setCommand(Tipo t){
        mensaje.put(0,(byte)t.v);
    }
    void addEntrada(Entrada e){
        /*--Entry table--*/
        mensaje.put(4+i*20,(byte)0);        //Address family
        mensaje.put(5+i*20,(byte)2);        //TODO http://tools.ietf.org/html/rfc1058 dice que 2 (¿?)
        mensaje.put(6+i*20,(byte)0);        //Route tag
        mensaje.put(7+i*20,(byte)0);
        mensaje.put(8+i*20,e.IPv4[0]);        //IPv4 address
        mensaje.put(9+i*20,e.IPv4[1]);
        mensaje.put(10+i*20,e.IPv4[2]);
        mensaje.put(11+i*20,e.IPv4[3]);
        mensaje.put(12+i*20,e.mascara[0]);       //Subnet mask
        mensaje.put(13+i*20,e.mascara[1]);
        mensaje.put(14+i*20,e.mascara[2]);
        mensaje.put(15+i*20,e.mascara[3]);
        mensaje.put(16+i*20,e.nextHoop[0]);   //Next hop
        mensaje.put(17+i*20,e.nextHoop[1]);
        mensaje.put(18+i*20,e.nextHoop[2]);
        mensaje.put(19+i*20,e.nextHoop[3]);
        mensaje.put(20+i*20,(byte)0);       //Metric
        mensaje.put(21+i*20,(byte)0);
        mensaje.put(22+i*20,(byte)0);
        mensaje.put(23+i*20,e.metrica);
        i++;
    }
    DatagramPacket generarDatagramPacket(InetAddress addrDestino,int puertoDestino){
            return new DatagramPacket(mensaje.array(),mensaje.limit(), addrDestino, puertoDestino);
    }
    DatagramPacket generarDatagramPacket(){
        try {
            return this.generarDatagramPacket(InetAddress.getByName("224.0.0.9"),520);//TODO cambiar puerto
        } catch (UnknownHostException e) {
            System.err.println("Direccion multicast mal"); //TODO eliminar
            return new DatagramPacket(new byte[0],1); //TODO Chapuza
        }
    }
}
