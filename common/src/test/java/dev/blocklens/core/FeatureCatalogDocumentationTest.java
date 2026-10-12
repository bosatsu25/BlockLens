package dev.blocklens.core;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
final class FeatureCatalogDocumentationTest {
 @Test void currentCatalogListsEveryControlExactlyOnceIncludingTerrain() throws Exception {
  String document=Files.readString(Path.of(System.getProperty("blocklens.repoRoot"),"knowledge/current/features.md"));
  var rows=document.lines().filter(line->line.startsWith("| ")&&(line.contains("/ Enabled")||line.contains("/ Disabled"))).toList();
  assertEquals(CapabilityId.values().length,rows.size());
  var names=new HashSet<String>();for(String row:rows)assertTrue(names.add(row.split("\\|")[2].strip()));
  assertTrue(rows.stream().anyMatch(row->row.contains("| Simplified Terrain |")&&row.contains("/ Disabled")));
 }
}
