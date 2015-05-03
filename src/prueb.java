import java.util.concurrent.TimeUnit;

/**
 * Created by javferrod on 3/05/15.
 */
public class prueb {

    public static void main (String [ ] args) {
        Tabla t = new Tabla();
        Entrada e = new Entrada("192.168.1.1","32",2);
        t.add(e);
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

    }
}
