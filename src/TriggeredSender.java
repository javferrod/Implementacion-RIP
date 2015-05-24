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
        Packet send;
        Random r = new Random();
        long elapsed;
        Entry e;

        try {
            e = triggeredPackets.take();
            Thread.sleep(10);
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

        triggeredPackets.drainTo(pendingTriggeredPackets);


    }

    private Packet getTriggeredPacket(Entry e) throws InterruptedException {
        Packet p = new Packet(Tipo.RESPONSE,pendingTriggeredPackets.size());
        pendingTriggeredPackets.forEach(p::addEntry);
        return p;
    }
}

