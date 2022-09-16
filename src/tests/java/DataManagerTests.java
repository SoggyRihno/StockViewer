import com.stockviewer.Data.DataManager;
import com.stockviewer.Data.APIInterval;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

public class DataManagerTests {

    @Test
    void getStockData() throws ExecutionException, InterruptedException {
        DataManager.getStockData("ibm", APIInterval.FIVE_MINUTES).get();

    }

}