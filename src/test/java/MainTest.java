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
import me.callsen.taylor.elevationGeoTiff.data.GraphDb;;

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
    // copy graph db from resource into temporary (to avoid persisting changes)
    FileUtils.copyDirectory(new File(classLoader.getResource("neo4j/graph.db").getFile()), tempDirectory.toFile());
    db = new GraphDb(tempDirectory.toFile().getAbsolutePath());
    geotiffFile = new GeoTiffFile(classLoader.getResource("tiff/USGS_13_n38w123-subsection-small-potrero.tif").getFile());

    // confirmed in @Test testSetElevationToRelationshipPage
    Main.processRelationshipPage(0, db, geotiffFile);
  }

  @AfterAll
  public void shutdownResources() {
    db.shutdown();
  }

  @Test
  public void testDbRelCount() throws Exception {
    assertEquals(676, db.getRelationshipCount());
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
      assertEquals(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, ((String[])rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_PROPERTY))[0]);
      
      // ad_elevation
      JSONArray elevationDataArray = new JSONArray((String)rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY));
      assertEquals(1, elevationDataArray.length());
      JSONObject elevationData = elevationDataArray.getJSONObject(0);
      assertEquals(-10.485306d, elevationData.getDouble("change"), ELEVATION_DOUBLE_PRECISION);
      assertEquals(49.25383377075195d, elevationData.getDouble("start"), ELEVATION_DOUBLE_PRECISION);
      assertEquals(38.76852798461914d, elevationData.getDouble("end"), ELEVATION_DOUBLE_PRECISION);
    }
    tx.close();
  }

  @Test
  public void testSetAssociatedDataSingleProp() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship)row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my\":\"data\"}]"));

      String[] associatedDataProps = (String[]) rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(2, associatedDataProps.length);
      assertEquals(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, associatedDataProps[0]);
      assertEquals("myProp", associatedDataProps[1]);

      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data", associatedData.getString("my"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataMultipleProp() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship)row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my\":\"data\"}]"));
      db.setAssociatedData(rel, "myProp2", new JSONArray("[{\"my2\":\"data2\"}]"));

      String[] associatedDataProps = (String[]) rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(3, associatedDataProps.length);
      assertEquals(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, associatedDataProps[0]);
      assertEquals("myProp", associatedDataProps[1]);
      assertEquals("myProp2", associatedDataProps[2]);

      // confirm myProp and myProp2 properties both set
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data", associatedData.getString("my"));
      associatedDataArray = new JSONArray((String) rel.getProperty("myProp2"));
      associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataMultipleSameProp() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship)row.get("r");

      // set single property twice
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my\":\"data\"}]"));
      db.setAssociatedData(rel, "myProp", new JSONArray("[{\"my2\":\"data2\"}]"));

      // confirm property is only added to associatedData once
      String[] associatedDataProps = (String[]) rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(2, associatedDataProps.length);
      assertEquals(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, associatedDataProps[0]);
      assertEquals("myProp", associatedDataProps[1]);

      // should still overwrite myProp
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }
  
  @Test
  public void testSetAssociatedDataJSONObject() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship)row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONObject("{\"my\":\"data\"}"));
      db.setAssociatedData(rel, "myProp2", new JSONObject("{\"my2\":\"data2\"}"));

      String[] associatedDataProps = (String[]) rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(3, associatedDataProps.length);
      assertEquals(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, associatedDataProps[0]);
      assertEquals("myProp", associatedDataProps[1]);
      assertEquals("myProp2", associatedDataProps[2]);

      // JSONObjects can be added separately and will be combined
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data", associatedData.getString("my"));
      associatedDataArray = new JSONArray((String) rel.getProperty("myProp2"));
      associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }

  @Test
  public void testSetAssociatedDataOverwrite() throws Exception {
    Transaction tx = db.getTransaction();
    Result result = tx.execute("MATCH ()-[r]-() WHERE r.start_osm_id=65312481 AND r.end_osm_id=65312480 return DISTINCT(r)");
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship rel = (Relationship)row.get("r");

      // set single property
      db.setAssociatedData(rel, "myProp", new JSONObject("{\"my\":\"data\"}"));
      db.setAssociatedData(rel, "myProp", new JSONObject("{\"my2\":\"data2\"}"));

      String[] associatedDataProps = (String[]) rel.getProperty(GraphDb.GRAPH_ASSOCIATED_DATA_PROPERTY);
      assertEquals(2, associatedDataProps.length); 
      assertEquals(GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, associatedDataProps[0]);
      assertEquals("myProp", associatedDataProps[1]); // will already have ad_elevation added & commited from first test

      // confirm myProp will be overwritten
      JSONArray associatedDataArray = new JSONArray((String) rel.getProperty("myProp"));
      JSONObject associatedData = associatedDataArray.getJSONObject(0);
      assertEquals("data2", associatedData.getString("my2"));
    }
    tx.rollback();
    tx.close();
  }
  
}
