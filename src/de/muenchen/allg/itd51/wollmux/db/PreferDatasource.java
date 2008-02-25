//TODO L.m()
/* 
* Dateiname: PreferDatasource.java
* Projekt  : WollMux
* Funktion : Datasource, die Daten einer Datenquelle von Datein einer andere
*            Datenquelle verdecken l�sst.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 07.11.2005 | BNK | Erstellung
* 11.11.2005 | BNK | getestet und debuggt
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * Datasource, die Daten einer Datenquelle A von Dateien einer anderen
 * Datenquelle B verdecken l�sst. Dies funktioniert so, dass Anfragen erst
 * an Datenquelle A gestellt werden und dann f�r alle Ergebnisdatens�tze
 * gepr�ft wird, ob ein Datensatz (oder mehrere Datens�tze) 
 * mit gleichem Schl�ssel in Datenquelle B ist. Falls dies so ist, werden
 * f�r diesen Schl�ssel nur die Datens�tze aus Datenquelle B zur�ckgeliefert.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PreferDatasource implements Datasource
{
  private Datasource source1;
  private Datasource source2;
  private String source1Name;
  private String source2Name;
  private Set<String> schema;
  private String name;
  
  /**
   * Erzeugt eine neue PreferDatasource.
   * @param nameToDatasource enth�lt alle bis zum Zeitpunkt der Definition
   *        dieser PreferDatasource bereits vollst�ndig instanziierten
   *        Datenquellen.
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser PreferDatasource enth�lt.
   * @param context der Kontext relativ zu dem URLs aufgel�st werden sollen
   *        (zur Zeit nicht verwendet).
   */
  public PreferDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException
  {
    try{ name = sourceDesc.get("NAME").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("NAME der Datenquelle fehlt");
    }
    
    try{ source1Name = sourceDesc.get("SOURCE").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("SOURCE der Datenquelle "+name+" fehlt");
    }
    
    try{ source2Name = sourceDesc.get("OVER").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("OVER-Angabe der Datenquelle "+name+" fehlt");
    }
    
    source1 = (Datasource)nameToDatasource.get(source1Name);  
    source2 = (Datasource)nameToDatasource.get(source2Name);
    
    if (source1 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source1Name+"\" nicht (oder fehlerhaft) definiert");
    
    if (source2 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source2Name+"\" nicht (oder fehlerhaft) definiert");

    /*
     * Anmerkung: Die folgende Bedingung ist "unn�tig" streng, aber um
     * sie aufzuweichen (z.B. Gesamtschema ist immer Schema von 
     * bevorzugter Datenquelle)
     * w�re es erforderlich, einen Dataset-Wrapper zu implementieren,
     * der daf�r sorgt, dass alle Datasets, die in QueryResults zur�ck-
     * geliefert werden das selbe Schema haben. Solange daf�r keine
     * Notwendigkeit ersichtlich ist, spare ich mir diesen Aufwand.
     */
    Set<String> schema1 = source1.getSchema();
    Set<String> schema2 = source2.getSchema();
    if (!schema1.containsAll(schema2) || !schema2.containsAll(schema1))
    {
      Set<String> difference1 = new HashSet<String>(schema1);
      difference1.removeAll(schema2);
      Set<String> difference2 = new HashSet<String>(schema2);
      difference2.removeAll(schema1);
      StringBuffer buf1 = new StringBuffer();
      Iterator<String> iter = difference1.iterator();
      while (iter.hasNext())
      {
        buf1.append(iter.next());
        if (iter.hasNext()) buf1.append(", ");
      }
      StringBuffer buf2 = new StringBuffer();
      iter = difference2.iterator();
      while (iter.hasNext())
      {
        buf2.append(iter.next());
        if (iter.hasNext()) buf2.append(", ");
      }
      throw new ConfigurationErrorException("Datenquelle \""+source1Name+"\" fehlen die Spalten: "+buf2+" und Datenquelle \""+source2Name+"\" fehlen die Spalten: "+buf1);
    }

    
    schema = new HashSet<String>(schema1);
  }

  public Set<String> getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout) throws TimeoutException
  {
    long endTime = System.currentTimeMillis() + timeout;
    QueryResults results = source2.getDatasetsByKey(keys, timeout);
    
    timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source2Name+" konnte Anfrage getDatasetsByKey() nicht schnell genug beantworten");
     
    QueryResults overrideResults = source1.getDatasetsByKey(keys, timeout);
    
    timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source1Name+" konnte Anfrage getDatasetsByKey() nicht schnell genug beantworten");
    
    return new QueryResultsOverride(results, overrideResults, source1, timeout);
  }
  
  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  public QueryResults find(List<QueryPart> query, long timeout) throws TimeoutException
  {
    long endTime = System.currentTimeMillis() + timeout;
    QueryResults results = source2.find(query, timeout);
    
    timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source2Name+" konnte Anfrage find() nicht schnell genug beantworten");
     
    QueryResults overrideResults = source1.find(query, timeout);
    
    timeout = endTime - System.currentTimeMillis();
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source1Name+" konnte Anfrage find() nicht schnell genug beantworten");
    
    return new QueryResultsOverride(results, overrideResults, source1, timeout);
  }

  public String getName()
  {
    return name;
  }

  private static class QueryResultsOverride implements QueryResults
  {
    private int size;
    private Set<String> keyBlacklist = new HashSet<String>();
    private QueryResults overrideResults;
    private QueryResults results;
    
    public QueryResultsOverride(QueryResults results, QueryResults overrideResults, Datasource override, long timeout)
    throws TimeoutException
    {
      this.overrideResults = overrideResults;
      
      long endTime = System.currentTimeMillis() + timeout;
      this.results = results;
      size = results.size();
        
      Map<String, int[]> keyToCount = new HashMap<String, int[]>(); //of int[]
      
      Iterator iter = results.iterator();
      while (iter.hasNext())
      { 
        Dataset ds = (Dataset)iter.next();
        String key = ds.getKey();
        if (!keyToCount.containsKey(key))
          keyToCount.put(key, new int[]{0});
        int[] count = keyToCount.get(key);
        ++count[0];
        if (System.currentTimeMillis() > endTime)
          throw new TimeoutException();
      }
      
      /**
       * Datens�tze f�r die ein Korrekturdatensatz vorliegt, dieaber nicht in
       * overrideResults auftauchen (weil die Korrektur daf�r gesorgt hat, dass
       * die Suchbedingung nicht mehr passt) m�ssen auch mit ihrem Schl�ssel
       * auf die Blacklist. Deswegen m�ssen wir diese Datens�tze suchen.
       */
      timeout = endTime - System.currentTimeMillis();
      if (timeout <= 0) throw new TimeoutException();
      QueryResults blacklistResults = override.getDatasetsByKey(keyToCount.keySet(),timeout); 
      
      size += overrideResults.size();
      
      QueryResults[] oResults = new QueryResults[]{overrideResults, blacklistResults};
      
      for (int i = 0; i < oResults.length; ++i)
      {
        iter = oResults[i].iterator();
        while (iter.hasNext())
        {
          Dataset ds = (Dataset)iter.next();
          String key = ds.getKey();
          
          int[] count = keyToCount.get(key);
          if (count != null)
          {
            size -= count[0];
            count[0] = 0;
            keyBlacklist.add(key);
          }
          if (System.currentTimeMillis() > endTime)
            throw new TimeoutException();
        }
      }
    }
    
    public int size()
    {
      return size;
    }

    public Iterator<Dataset> iterator()
    {
      return new MyIterator();
    }

    public boolean isEmpty()
    {
      return size == 0;
    }
    
    private class MyIterator implements Iterator<Dataset>
    {
      private Iterator<Dataset> iter;
      private boolean inOverride;
      private int remaining;
      
      public MyIterator()
      {
        iter = overrideResults.iterator();
        inOverride = true;
        remaining = size;
      }
      
      public void remove()
      {
        throw new UnsupportedOperationException();
      }

      public boolean hasNext()
      {
        return (remaining > 0);
      }

      public Dataset next()
      {
        if (remaining == 0)
          throw new NoSuchElementException();
        
        --remaining;
        
        if (inOverride)
        {
          if (iter.hasNext()) return iter.next();
          inOverride = false;
          iter = results.iterator();
        }
        
        Dataset ds;
        do{
          ds = iter.next();
        }while (keyBlacklist.contains(ds.getKey()));
        
        return ds;
      }
    }
  }
  
}
