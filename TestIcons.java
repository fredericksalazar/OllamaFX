import org.kordamp.ikonli.IkonHandler;
import java.util.ServiceLoader;
public class TestIcons {
    public static void main(String[] args) {
        ServiceLoader<IkonHandler> loader = ServiceLoader.load(IkonHandler.class);
        for (IkonHandler handler : loader) {
            System.out.println(handler.getClass().getName());
        }
    }
}
