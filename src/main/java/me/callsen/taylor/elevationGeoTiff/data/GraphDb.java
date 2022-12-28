package me.callsen.taylor.elevationGeoTiff.data;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class GraphDb {

  private enum NodeLabels implements Label { INTERSECTION; } 
  private enum RelationshipTypes implements RelationshipType { CONNECTS; }
  
  public GraphDatabaseService db;
  private DatabaseManagementService managementService;
  private Transaction sharedTransaction;
  
  // define number of transactions accepted before commiting (excluding index and truncate function which commit no matter what)
  private static final int SHARED_TRANSACTION_COMMIT_INTERVAL = 5000;
  private int sharedTransactionCount = 0;

  private static final int GRAPH_PAGINATION_AMOUNT = 5000;

  public static final String GRAPH_ASSOCIATED_DATA_PROPERTY = "associatedData";
  public static final String GRAPH_GEOM_PROPERTY = "geom";
  public static final String GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY = "ad_elevation";

  public GraphDb(String graphDbPath) throws Exception {
    
    // initialize graph db connection
    managementService = new DatabaseManagementServiceBuilder( Paths.get( graphDbPath ) ).build();
    db = managementService.database( DEFAULT_DATABASE_NAME );
    // db = new GraphDatabaseFactory().newEmbeddedDatabase( new File( graphDbPath ) );
    System.out.println("Graph DB @ " + graphDbPath + " initialized");
    
    // initialize shared transaction - bundles multiple transactions into single commit to improve performance
    this.sharedTransaction = this.db.beginTx();

  }

  public Transaction getSharedTransaction() {
    if (this.sharedTransaction != null) {
      return sharedTransaction;
    } else {
      this.sharedTransaction = this.db.beginTx();
      return this.sharedTransaction;
    }
  }

  // overloaded for convienence
  public void commitSharedTransaction() {
    this.commitSharedTransaction(true);
  }

  public void commitSharedTransaction(boolean openNewTransaction) {
    try {
      // commit pending transaction
      this.sharedTransaction.commit();
      this.sharedTransaction.close();
      // reset transaction count to 0
      this.sharedTransactionCount = 0;
    } catch (Exception e) { 
      System.out.println("Warning - failed to commit shared transaction (not necessarily an issue)"); 
      e.printStackTrace();
    } finally {
      // open a new transaction, or unassign (used at app shutdown)
      if (openNewTransaction) this.sharedTransaction = this.db.beginTx();
      else this.sharedTransaction = null;
    }
  }

  public void shutdown(){
    
    // commit shared transaction
    this.commitSharedTransaction(false);

    // close db connection
    this.managementService.shutdown();
    
    System.out.println("Graph DB shutdown");

  }

  public long getRelationshipCount() {

    long count = 0;

    Transaction tx = this.db.beginTx();
    try ( Result result = tx.execute( "MATCH ()-[r]-() RETURN COUNT(r) AS total" ) ) {
      while ( result.hasNext() ) {
        Map<String, Object> row = result.next();
        count = (Long) row.get("total");
      }
    } finally {
      tx.close();  
    }

    return count;
  }

  public Result getRelationshipPage(Transaction tx, int pageNumber) {
    long startIndex = pageNumber * GRAPH_PAGINATION_AMOUNT;
    Result result = tx.execute( String.format("MATCH ()-[r]-() RETURN r as way ORDER BY r.osm_id DESC SKIP %s LIMIT %s", startIndex, GRAPH_PAGINATION_AMOUNT ) );
    return result;
  }

  public void setAssociatedData(Relationship relationship, String propertyName, JSONObject associatedData) {
    // add property to associatedData list
    addAssociatedDataProperty(relationship, propertyName);

    JSONArray associatedDataArray = new JSONArray();
    associatedDataArray.put(associatedData);

    // add JSON data to associated data property 
    relationship.setProperty(propertyName, associatedDataArray.toString());
  }

  public void setAssociatedData(Relationship relationship, String propertyName, JSONArray associatedData) {
    // add property to associatedData list
    addAssociatedDataProperty(relationship, propertyName);

    // add JSON data to associated data property 
    relationship.setProperty(propertyName, associatedData.toString());
  }

  private void addAssociatedDataProperty(Relationship relationship, String propertyName) {
    // add referenced associated data property to associatedData array (or create one of doesn't exist yet)
    //  TODO: ensure duplicate entries can not be added (possibly using Set)
    String[] associatedDataArray;
    if (relationship.hasProperty(GRAPH_ASSOCIATED_DATA_PROPERTY)) {
      ArrayList<String> existingAssociatedDataList = new ArrayList<String>(Arrays.asList((String[]) relationship.getProperty(GRAPH_ASSOCIATED_DATA_PROPERTY)));
      existingAssociatedDataList.add(propertyName);
      associatedDataArray = existingAssociatedDataList.stream().toArray(String[]::new);
    } else {
      associatedDataArray = new String[1];
      associatedDataArray[0] = propertyName ;
    }
    relationship.setProperty(GRAPH_ASSOCIATED_DATA_PROPERTY, associatedDataArray);
  }

}