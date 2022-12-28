# Load GeoTiff Elvation Data to Graph

Module to load elevation data from USGS GeoTiff file into a Graph database. Works by querying all relationships in Graph, iterating over reach, and looking up elevation in the GeoTiff. 

Tested with GeoTiff files from USGS [The National Map](https://www.usgs.gov/the-national-map-data-delivery).

## Building

The `.pom` file is set to compile a standalone `.jar` file.

```
mvn clean install
```

## Running

Make sure the configure file is set, and the Postgres instance is connectable.

```
java -jar target/elevationGeoTiff-0.0.1-SNAPSHOT.jar /development/workspace/USGS_13_n38w123_uncomp.tif /development/workspace/neo4j/graph.db
```


# Thank you

Special thank you to these posts/resources that helped overcome strange hurdles:

- Executable `.jar` would break when attempting to load GeoTiff - required shading rule in `pom.xml` - [https://johnewart.net/2012/geotools-maven-and-jai-oh-my/](https://johnewart.net/2012/geotools-maven-and-jai-oh-my/)