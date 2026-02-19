import org.kordamp.ikonli.IkonHandler;
import org.kordamp.ikonli.Ikon;
import java.util.ServiceLoader;

public class TestIconNames {
    public static void main(String[] args) {
        ServiceLoader<IkonHandler> loader = ServiceLoader.load(IkonHandler.class);
        for (IkonHandler handler : loader) {
            String handlerName = handler.getClass().getSimpleName();
            try {
                Class<?> handlerClass = handler.getClass();
                // Try to get font alias
                System.out.println("Handler: " + handlerName + ", Font: " + handler.getFontFamily());
            } catch (Exception e) {}
        }
    }
}
