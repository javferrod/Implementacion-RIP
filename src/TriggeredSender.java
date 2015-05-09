import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;


public class TriggeredSender implements Runnable {

    LinkedBlockingQueue<Entry> triggeredPackets;
    private LinkedList<Entry> pendingTriggeredPackets = new LinkedList<>();


    private long timer;

    TriggeredSender(LinkedBlockingQueue<Entry> link){
        this.triggeredPackets =link;
    }

    @Override
    public void run() {
        Entry e = null;
        timer = System.nanoTime();

        while(true) {
            try {
                e = triggeredPackets.take();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            Random r = new Random();
            long elapsed = System.nanoTime() - (timer + (1 + r.nextInt(4)) * 1000000000L);

            if (elapsed > 5000000000L) {
                System.out.println("[Triggered Update] Sending");
                RipServer.sendUnicast(getTriggeredPacket());
                timer = System.nanoTime();
            } else {
                System.out.println("[Triggered Update] Waiting for timeout");
                pendingTriggeredPackets.add(e);
                triggeredPackets.drainTo(pendingTriggeredPackets);
                synchronized (this) {
                    try {
                        Thread.sleep(elapsed * 1000 + r.nextInt(5) * 1000);
                        RipServer.sendUnicast(getTriggeredPacket());
                    } catch (InterruptedException ignored) {
                        ignored.printStackTrace();
                    }
                }
            }

        }

        }

    private Packet getTriggeredPacket(){
        int pendingSize = pendingTriggeredPackets.size();
        int blockingSize = triggeredPackets.size();
        int totalSize = pendingSize+blockingSize;
        if(pendingSize>25) pendingSize=25;
        if(totalSize>25) {
            blockingSize = 25 - pendingSize;
            totalSize = 25;
        }

        Packet p = new Packet(Tipo.RESPONSE,totalSize);

        for (int i = 0; i < pendingSize ; i++) {
            p.addEntry(pendingTriggeredPackets.pop());
        }
        for (int i = 0; i < blockingSize; i++) {
            p.addEntry(triggeredPackets.poll());
        }

        return p;
    }

}
