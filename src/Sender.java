import java.util.Random;


public class Sender implements Runnable {

    Table entryTable;
    Thread receiverThread;

    private int timeOut;

    Sender(Table entryTable, int timeout, Thread toInterrupt){
        this.timeOut = timeout;
        this.entryTable=entryTable;
        this.receiverThread = toInterrupt;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Random r = new Random();
                Thread.sleep(timeOut * 1000 + r.nextInt(5) * 1000);
            } catch (InterruptedException ignored) {
            }
            receiverThread.interrupt();
            RipServer.sendUnicast(updateMessage());
        }

    }

    private Packet updateMessage() {
        System.out.println(" [Mensaje ordinario] Enviando tabla de encaminamiento...");
        System.out.println("-------------------ESTADO DE LA TABLA-------------------");
        entryTable.forEach(System.out::println);
        Packet p = new Packet(Tipo.RESPONSE, entryTable.size());
        entryTable.forEach(p::addEntry);
        return p;
    }


}
