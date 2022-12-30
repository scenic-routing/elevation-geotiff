import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;

import me.callsen.taylor.elevationGeoTiff.data.GraphDb;;

@TestInstance(Lifecycle.PER_CLASS)
public class MainTest {
  
  @TempDir
  private static Path tempDirectory;

  @Test
  public void testNodeCount() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();

    // copy graph db from resource into temporary (to avoid persisting changes)
    FileUtils.copyDirectory(new File(classLoader.getResource("neo4j/graph.db").getFile()), tempDirectory.toFile());

    GraphDb db = new GraphDb(tempDirectory.toFile().getAbsolutePath());
    
    assertEquals(676, db.getRelationshipCount());
    
    db.shutdown();
  }
  
}
