package me.callsen.taylor.elevationGeoTiff;

import java.util.Map;

import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.json.JSONObject;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.spatial.Point;
import org.opengis.referencing.operation.TransformException;

import me.callsen.taylor.elevationGeoTiff.data.GeoTiffFile;
import me.callsen.taylor.elevationGeoTiff.data.GraphDb;

public class Main {

  // TODO: move to shared
  private static final int GRAPH_PAGINATION_AMOUNT = 5000;
  public static void main( String[] args ) throws Exception {

    // parameters
    //  ensure required args are specified - otherwise exit
    if (args.length < 2) {
      System.out.println("Required paramters not specified - exiting");
      System.exit(1);
    }
    String geoTiffPath = args[0];
    String graphDbPath = args[1];
    System.out.println("Elevation (GeoTiff) to Graph Initialized with following parameters: ");
    System.out.println("geoTiffPath: " + geoTiffPath);
    System.out.println("    graphDb: " + graphDbPath);

    // initialize graphDB and utils
    GraphDb graphDb = new GraphDb(graphDbPath);

    // initialize geotiff
    GeoTiffFile geotiffFile = new GeoTiffFile(geoTiffPath);

    // loop through all relationships in graph and assign elevation data
    long totalRelCount = graphDb.getRelationshipCount();
    for (int pageNumber = 0; pageNumber * GRAPH_PAGINATION_AMOUNT < totalRelCount; ++pageNumber) {
      System.out.println(String.format("Processing page %s, up to relationship %s", pageNumber, (pageNumber + 1) * GRAPH_PAGINATION_AMOUNT));
      processRelationshipPage(pageNumber, graphDb, geotiffFile);
    }

    // shutdown GraphDB
    graphDb.shutdown();

    System.out.println("Task complete");

  }

  public static void processRelationshipPage(int pageNumber, GraphDb graphDb, GeoTiffFile geoTiffFile) throws InvalidGridGeometryException, TransformException {
    
    Transaction tx = graphDb.getSharedTransaction();
    Result result = graphDb.getRelationshipPage(tx, pageNumber);

    // loop through relationships returned in page
    while ( result.hasNext() ) {
      Map<String, Object> row = result.next();
      Relationship relationship = (Relationship)row.get("way");

      // retrieve geometry and query geotiff for elevation of points - support possibility of more than 2 points
      Point[] geomPoints = (Point[]) relationship.getProperty(GraphDb.GRAPH_GEOM_PROPERTY);
      Point startPoint = geomPoints[0];
      double startElevation = geoTiffFile.getData(startPoint.getCoordinate().getCoordinate().get(0), startPoint.getCoordinate().getCoordinate().get(1));
      Point endPoint = geomPoints[geomPoints.length-1];
      double endElevation = geoTiffFile.getData(endPoint.getCoordinate().getCoordinate().get(0), endPoint.getCoordinate().getCoordinate().get(1));

      // assemble json data
      JSONObject elevationData = new JSONObject();
      elevationData.put("start", startElevation);
      elevationData.put("end", endElevation);
      elevationData.put("change", (endElevation - startElevation));

      // set elevation to associated data in graph
      graphDb.setAssociatedData(relationship, GraphDb.GRAPH_ASSOCIATED_DATA_ELEVATION_PROPERTY, elevationData);
    }

    graphDb.commitSharedTransaction();

  }

}