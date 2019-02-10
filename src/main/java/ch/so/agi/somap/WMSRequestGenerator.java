package ch.so.agi.somap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WMSRequestGenerator {
    Logger log = LoggerFactory.getLogger(this.getClass());
      
    public void createCSV(int count, double minres, double maxres, String minsize, String maxsize, String region) {
        String[] regionParts = region.split(",");
        Envelope regionEnvelope = new Envelope(Double.valueOf(regionParts[0]), Double.valueOf(regionParts[2]),
                Double.valueOf(regionParts[1]), Double.valueOf(regionParts[3]));
        log.debug(regionEnvelope.toString());

        GeometryFactory geomFactory = new GeometryFactory();
        Polygon regionPolygon = (Polygon) geomFactory.toGeometry(regionEnvelope);
        Polygon[] regionPolygons = new Polygon[1];
        regionPolygons[0] = regionPolygon;
        MultiPolygon regionMultiPolygon = geomFactory.createMultiPolygon(regionPolygons);
        log.debug(regionMultiPolygon.toString());
        
        this.createCSV(count, minres, maxres, minsize, maxsize, regionMultiPolygon);
    }
    
    public void createCSV(int count, double minres, double maxres, String minsize, String maxsize, File shpFile) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("url", shpFile.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];
            log.debug(typeName);
            
            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();

            MultiPolygon perimeter = null;
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    perimeter = (MultiPolygon) feature.getDefaultGeometryProperty().getValue();
                    break; // only one geometry is supported
                }
            }
            
            this.createCSV(count, minres, maxres, minsize, maxsize, perimeter);
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }

    private void createCSV(int count, double minres, double maxres, String minsize, String maxsize, MultiPolygon perimeter) {
        String[] minsizeParts = minsize.split(",");
        String[] maxsizeParts = maxsize.split(",");
        
        int minsizeX = Integer.valueOf(minsizeParts[0]);
        int minsizeY = Integer.valueOf(minsizeParts[1]);
        
        int maxsizeX = Integer.valueOf(maxsizeParts[0]);
        int maxsizeY = Integer.valueOf(maxsizeParts[1]);

        int i=0;
        ArrayList<String> csvLines = new ArrayList<String>();
        while (count > 0) {
            i++;
            log.debug(String.valueOf(count));
            
            Random r = new Random();
            int width = r.nextInt((maxsizeX - minsizeX) + 1) + minsizeX;
            int height = r.nextInt((maxsizeY - minsizeY) + 1) + minsizeY;

            Envelope envelope = perimeter.getEnvelopeInternal();
            
            double centerX = Math.random() * (envelope.getMaxX() - envelope.getMinX()) + envelope.getMinX();
            double centerY = Math.random() * (envelope.getMaxY() - envelope.getMinY()) + envelope.getMinY();

            log.debug(String.valueOf(centerX));
            log.debug(String.valueOf(centerY));
            
            double maxLog = log2(maxres);
            double minLog = log2(minres);
            double randomLog = Math.random() * (maxLog - minLog) + minLog;
            double res = Math.pow(2, randomLog);
            log.debug(String.valueOf(res));
            
            Envelope bboxEnvelope = new Envelope(centerX - width*0.5*res, centerX + width*0.5*res, 
                    centerY - height*0.5*res, centerY + height*0.5*res);
            log.debug(bboxEnvelope.toString());
            
            GeometryFactory geomFactory = new GeometryFactory();
            Polygon bboxPolygon = (Polygon) geomFactory.toGeometry(bboxEnvelope);
//            boolean intersects = perimeter.intersects(bboxPolygon);
            boolean within = bboxPolygon.within(perimeter);
            log.debug(String.valueOf(within));
            
            if (within) {
                StringBuilder line = new StringBuilder();
                line.append(String.valueOf(width));
                line.append(",");
                line.append(String.valueOf(height));
                line.append(",");
                line.append(String.format("%.8f", bboxEnvelope.getMinX()));
                line.append(",");
                line.append(String.format("%.8f", bboxEnvelope.getMinY()));
                line.append(",");
                line.append(String.format("%.8f", bboxEnvelope.getMaxX()));
                line.append(",");
                line.append(String.format("%.8f", bboxEnvelope.getMaxY()));

                csvLines.add(line.toString());
                //log.info(line.toString());

                count = count - 1;
            }   
        }
        log.info("******"+i);
    }

    private double log2(double n) {
        return (Math.log(n) / Math.log(2));
    }
}
