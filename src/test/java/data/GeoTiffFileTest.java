package data;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import me.callsen.taylor.elevationGeoTiff.data.GeoTiffFile;

@TestInstance(Lifecycle.PER_CLASS)
public class GeoTiffFileTest {
  
  private GeoTiffFile geotiffFile;

  @BeforeAll
  public void initResources() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    geotiffFile = new GeoTiffFile(classLoader.getResource("tiff/USGS_13_n38w123-subsection-small-potrero.tif").getFile());
  }

  @Test
  public void getDataOutsideBounds() throws Exception {
    Double elevation = geotiffFile.getData(15d, 15d);
    assertNull(elevation);
  }
}
