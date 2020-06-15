package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;

class RAMDatasourceTest
{

  @Test
  void testRAMDatasource()
  {
    Datasource ds = new RAMDatasource("ram", List.of("column"),
        List.of(new MockDataset(), new MockDataset("ds3", "column", "value3")));
    assertEquals(List.of("column"), ds.getSchema());
    assertEquals("ram", ds.getName());
    QueryResults results = ds.getContents();
    assertEquals(2, results.size());
    results = ds.getDatasetsByKey(List.of("ds", "ds2"));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
    results = ds.find(List.of());
    assertEquals(0, results.size());
    results = ds.find(List.of(new QueryPart("column", "value2")));
    assertEquals(0, results.size());
  }

  @Test
  void testUninitialized()
  {
    Datasource ds = new RAMDatasource();
    assertNull(ds.getName());
    assertThrows(NullPointerException.class, () -> ds.getSchema());
  }

}