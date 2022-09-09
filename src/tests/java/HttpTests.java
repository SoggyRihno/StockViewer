import com.stockviewer.API.APIManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HttpTests {
    @Test
    void testHttp() throws ExecutionException, InterruptedException {
        CompletableFuture<String> result = APIManager.getRateLimited("https://jsonplaceholder.typicode.com/posts");
        String sha256hex = DigestUtils.sha256Hex(result.get());
        assertEquals(sha256hex,"35d44a4bde6d5614da88808ee6bd5a10a0414cf13c17645dbc3019a51064e87d");
    }




}
