# Load GeoTiff Elevation Data to Graph

Module to load elevation data from a USGS GeoTiff file into a Graph database. Works by querying all relationships in the Graph, iterating over each, and looking up elevation in the GeoTiff (based on relationship lat/long GPS coordinates stored in the `geom` property). This module is designed to be run on Graph data loaded by the [osm2graph-neo4j](https://github.com/scenic-routing/osm2graph-neo4j) module.

Tested with GeoTiff files from USGS [The National Map](https://www.usgs.gov/the-national-map-data-delivery). **Note:** The [GeoTools library](https://www.geotools.org/) used for GeoTiff parsing does not support GeoTiff compression, so make sure any [GeoTiff files are uncompressed](https://gis.stackexchange.com/questions/92608/decompress-a-lzw-compressed-geotiff).

## Building

The `.pom` file is set to compile a standalone `.jar` file.

```
mvn clean install
```

## Running

Use the follow command to execute the module, with the `.tif` file and `graph.db` paths supplied as parameters.

```
java -jar target/elevationGeoTiff-0.0.1-SNAPSHOT.jar /development/workspace/USGS_13_n38w123_uncomp.tif /development/workspace/neo4j/graph.db
```

The Graph database is expected to have an array of Neo4j Spatial `Point` datatypes populated on the relationship `geom` property. See the schema created by the [osm2graph-neo4j](https://github.com/scenic-routing/osm2graph-neo4j) module for more details.

## Testing

Tests can be executed with the following command:

```
mvn test
```

# Thank you

Special thank you to these posts/resources that helped overcome strange hurdles:

- Executable `.jar` would break when attempting to load GeoTiff - required shading rule in `pom.xml` - [https://johnewart.net/2012/geotools-maven-and-jai-oh-my/](https://johnewart.net/2012/geotools-maven-and-jai-oh-my/)

# Supplemental Blog Post

I wrote a blog post about the research and findings encountered as part of creating this module: [https://taylor.callsen.me/parsing-geotiff-files-in-java/](https://taylor.callsen.me/parsing-geotiff-files-in-java/)
