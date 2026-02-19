import javafx.application.Platform;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.concurrent.CountDownLatch;

public class TestFA {
    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FontIcon icon = new FontIcon("fab-paypal");
                System.out.println("Loaded: " + icon.getIconCode());
                FontIcon icon2 = new FontIcon("fas-coffee");
                System.out.println("Loaded: " + icon2.getIconCode());
                FontIcon icon3 = new FontIcon("fth-heart");
                System.out.println("Loaded: " + icon3.getIconCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        });
        latch.await();
        System.exit(0);
    }
}
