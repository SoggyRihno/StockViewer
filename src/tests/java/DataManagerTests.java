import com.stockviewer.Data.DataManager;
import com.stockviewer.Data.Interval;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

public class DataManagerTests {

    @Test
    void getStockData() throws ExecutionException, InterruptedException {
        DataManager.getStockData("ibm", Interval.FIVE_MINUTES).get();

    }

}