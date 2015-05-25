import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;


public class TriggeredSender implements Runnable {

    LinkedBlockingQueue<Entry> triggeredPackets;
    private LinkedList<Entry> pendingTriggeredPackets = new LinkedList<>();


    TriggeredSender(LinkedBlockingQueue<Entry> link) {
        this.triggeredPackets = link;
    }

    @Override
    public void run() {
        long timer = System.nanoTime();
        long timeToWait;
        Random r = new Random();
        long elapsed;
        Entry e;
        while(true) {
            pendingTriggeredPackets = new LinkedList<>();
            try {
                e = triggeredPackets.take();
                pendingTriggeredPackets.add(e);
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }

            elapsed = System.nanoTime() - timer;
            timeToWait = (1 + r.nextInt(4)) * 1000000000L;

            if (elapsed < timeToWait) {
                try {
                    Thread.sleep(timeToWait - elapsed);
                } catch (InterruptedException ignored) {
                }
            }
            System.out.println("TRIGGERED UPDATE");
            triggeredPackets.drainTo(pendingTriggeredPackets);
            RipServer.sendUnicast(getTriggeredPacket());
        }
    }

    private Packet getTriggeredPacket() {
        Packet p = new Packet(Tipo.RESPONSE,pendingTriggeredPackets.size());
        System.err.println("-----TRIGGERED-------");

        for (Entry e: pendingTriggeredPackets){
            System.out.println(e);
            p.addEntry(e);
        }
        System.out.println("--------------------");

        return p;
    }
}

