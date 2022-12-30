package me.callsen.taylor.elevationGeoTiff.data;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class GeoTiffFile {

  private Raster tiffRaster;
  private GridGeometry2D gridGeometry;

  public GeoTiffFile(String geotiffFilePath) throws IOException {
    File tiffFile = new File(geotiffFilePath);
    GeoTiffReader reader = new GeoTiffReader(tiffFile);
    GridCoverage2D cov = reader.read(null);
    gridGeometry = cov.getGridGeometry();

    tiffRaster = cov.getRenderedImage().getData();

    System.out.println("GeoTiff file @ " + geotiffFilePath + " initialized");
  }

  @Nullable
  public Double getData(double longitude, double latitude) {
    try {
      // convert lat/long to pixel coordinates
      CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84;
      DirectPosition2D posWorld = new DirectPosition2D(wgs84, longitude, latitude); // longitude supplied first
      GridCoordinates2D posGrid = gridGeometry.worldToGrid(posWorld);

      // sample tiff data with at pixel coordinate - only retrieving data from first band
      double[] rasterData = new double[1];
      tiffRaster.getPixel(posGrid.x, posGrid.y, rasterData);
      return rasterData[0];

    } catch (Exception e) {  
      // return null value if error occurs (e.g. requesting elevation outside of raster pixel range)
      return null;
    }
  }
  
}
