import com.stockviewer.Data.StockData;
import com.stockviewer.Exceptions.APIException;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

public class DataParsingTest {
    @Test
    void testParsing() throws Exception {
        Stream.of("ibm","gme","","asdas","2342d").map(i-> {try {return StockData.newStockData(i);} catch (APIException e) {return null;}}).toList();
    }
}
