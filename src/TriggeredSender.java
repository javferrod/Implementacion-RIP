import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;


public class TriggeredSender implements Runnable {

    LinkedBlockingQueue<Entry> triggeredPackets;
    private LinkedList<Entry> pendingTriggeredPackets = new LinkedList<>();


    TriggeredSender(LinkedBlockingQueue<Entry> link){
        this.triggeredPackets =link;
    }

    @Override
    public void run() {
        Entry e = null;
        long timer = System.nanoTime();

        while(true) {
            try {
                pendingTriggeredPackets.add(triggeredPackets.take());
            } catch (InterruptedException e1) {
                System.out.println("Interrupted");
            }

            Random r = new Random();
            long elapsed = System.nanoTime() - timer;
            long timeToWait = (1 + r.nextInt(4)) * 1000000000L;

            if (elapsed > timeToWait) {
                System.out.println("[Triggered Update] Sending");
                RipServer.sendUnicast(getTriggeredPacket());
                timer = System.nanoTime();
            } else {
                System.out.println("[Triggered Update] Waiting for timeout");
                triggeredPackets.drainTo(pendingTriggeredPackets);
                synchronized (this) {
                    try {
                        Thread.sleep(timeToWait - elapsed);
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
        for (int j = 0; j < blockingSize; j++) {
            p.addEntry(triggeredPackets.poll());
        }

        return p;
    }

}
