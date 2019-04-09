package ch.so.agi.somap;

import java.io.File;

import org.junit.jupiter.api.Test;

public class WMSRequestGeneratorTest {
    //@Test 
    public void createRequestsFromBoundingBox_Ok() {
        WMSRequestGenerator requestGenerator = new WMSRequestGenerator();
        requestGenerator.createCSV(1000, 0.01, 20, "1920,1080", "2560,1440", "2590000,1210000,2650000,1270000");
        
        
    }
    
    @Test 
    public void createRequestsFromShapefile_Ok() {
        WMSRequestGenerator requestGenerator = new WMSRequestGenerator();
        requestGenerator.createCSV(1000, 0.01, 20, "1920,1080", "2560,1440", new File("src/test/data/agi_hoheitsgrenzen_pub_hoheitsgrenzen_kantonsgrenze.shp"));

    }
}
