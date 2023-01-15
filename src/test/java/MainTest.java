import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import me.callsen.taylor.elevationGeoTiff.Main;
import me.callsen.taylor.elevationGeoTiff.data.GeoTiffFile;
import me.callsen.taylor.scenicrouting.javasdk.data.GraphDb;
import me.callsen.taylor.scenicrouting.javasdk.RoutingConstants;
import me.callsen.taylor.scenicrouting.javasdk.TestUtils;

@TestInstance(Lifecycle.PER_CLASS)
public class MainTest {
  
  @TempDir
  private static Path tempDirectory;

  private GraphDb db;

  private GeoTiffFile geotiffFile;

  private static final double ELEVATION_DOUBLE_PRECISION = .0009d;

  @BeforeAll
  public void initResources() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();

    db = TestUtils.getLoadedGraphDb();
    geotiffFile = new GeoTiffFile(classLoader.getResource("tiff/USGS_13_n38w123-subsection-small-potrero.tif").getFile());

    // confirmed in @Test testSetElevationToRelationshipPage
    Main.processRelationshipPage(0, db, geotiffFile);
  }

  @AfterAll
  public void shutdownResources() {
    db.shutdown();
  }

  @Test
  public void testSetElevationToRelationshipPage() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65320204 return DISTINCT(r)");
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship)row.get("r");
      
      // osm_id
      assertEquals(278713838, rel.getProperty("osm_id"));
      
      // associatedData
      assertEquals(Main.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, ((String[])rel.getProperty(RoutingConstants.GRAPH_PROPERTY_NAME_ASSOCIATED_DATA))[0]);
      
      // ad_elevation
      JSONArray elevationDataArray = new JSONArray((String)rel.getProperty(Main.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY));
      assertEquals(1, elevationDataArray.length());
      JSONObject elevationData = elevationDataArray.getJSONObject(0);
      assertEquals(-10.485306d, elevationData.getDouble("change"), ELEVATION_DOUBLE_PRECISION);
      assertEquals(49.25383377075195d, elevationData.getDouble("start"), ELEVATION_DOUBLE_PRECISION);
      assertEquals(38.76852798461914d, elevationData.getDouble("end"), ELEVATION_DOUBLE_PRECISION);
    }
    tx.close();
  }

}
