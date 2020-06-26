/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Basisklasse für DJDataset-Implementierungen.
 */
public abstract class DJDatasetBase implements DJDataset
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DJDatasetBase.class);

  /**
   * Bildet Spaltennamen auf (String-)Werte ab. Die Daten in myLOS repräsentieren den
   * lokalen Override, die in myBS die (gecachten) Daten aus der
   * Hintergrunddatenbank. myLOS kann null sein, dann wird der Datensatz als nicht
   * aus dem LOS kommend betrachtet.
   */
  protected Map<String, String> myLOS;

  /**
   * Bildet Spaltennamen auf (String-)Werte ab. Die Daten in myLOS repräsentieren den
   * lokalen Override, die in myBS die (gecachten) Daten aus der
   * Hintergrunddatenbank. myBS kann null sein, dann wird der Datensatz als nur aus
   * dem LOS kommend und nicht mit einer Hintergrunddatenbank verknüpft betrachtet.
   */
  protected Map<String, String> myBS;

  /**
   * Die Menge aller Spaltennamen, die dieser Datensatz kennt. Falls dieses Set nicht
   * null ist, werfen Versuche, auf unbekannte Spalten zuzugreifen eine Exception.
   */
  protected List<String> schema;

  /**
   * Erzeugt einen neuen Datensatz.
   * 
   * @param backingStore
   *          mappt Spaltennamen auf den Spaltenwert des Datensatzes in der
   *          Hintergrunddatenbank. Spalten, die nicht enthalten sind werden als im
   *          Datensatz unbelegt betrachtet. Als backingStore kann null übergeben
   *          werden (für einen Datensatz, der nur aus dem LOS kommt ohne
   *          Hintergrundspeicher).
   * @param overrideStore
   *          mappt Spaltenname auf den Spaltenwert im LOS- Ist eine Spalte nicht
   *          vorhanden, so hat sie keinen Override im LOS. Wird für overrideStore
   *          null übergeben, so wird der Datensatz als nicht aus dem LOS kommend
   *          betrachtet.
   * @param schema
   *          falls nicht null übergeben wird, erzeugen Zugriffe auf Spalten mit
   *          Namen, die nicht in schema sind Exceptions.
   */
  public DJDatasetBase(Map<String, String> backingStore,
      Map<String, String> overrideStore, List<String> schema)
  {
    myBS = backingStore;
    myLOS = overrideStore;
    this.schema = schema;
  }

  @Override
  public String toString()
  {
    StringBuilder stringBuilder = new StringBuilder();

    try
    {
      String rolle = get("Rolle");
      String nachname = get("Nachname");
      String vorname = get("Vorname");

      stringBuilder.append(rolle == null || rolle.isEmpty() ? "" : "(" + rolle + ") ");
      stringBuilder.append(nachname == null || nachname.isEmpty() ? "" : nachname);
      stringBuilder.append(", ");
      stringBuilder.append(vorname == null || vorname.isEmpty() ? "" : vorname);
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return stringBuilder.toString();
  }

  /**
   * Liefert die Map, die dem Konstruktor als backingStore Argument übergeben wurde.
   */
  public Map<String, String> getBS()
  {
    return myBS;
  }

  /**
   * Liefert die Map, die dem Konstruktor als overrideStore Argument übergeben wurde.
   */
  public Map<String, String> getLOS()
  { // TESTED
    return myLOS;
  }

  /**
   * Liefert den Wert, den dieser Datensatz in der Spalte spaltenName stehen hat
   * (override hat vorrang vor backing store). Falls schema nicht null ist, wird eine
   * ColumnNotFoundException geworfen, wenn versucht wird auf eine Spalte
   * zuzugreifen, die nicht im schema ist. Falls weder backing store noch override
   * Speicher einen Wert für diese Spalte haben, so wird null geliefert (nicht
   * verwechseln mit Zugriff auf eine nicht existierende Spalte!).
   */
  @Override
  public String get(String spaltenName) throws ColumnNotFoundException
  {
    if (schema != null && !schema.contains(spaltenName))
      throw new ColumnNotFoundException(L.m("Spalte %1 existiert nicht!",
        spaltenName));
    String res;
    res = myLOS.get(spaltenName);
    if (res != null){
      return res;
    }
    if (myBS != null)
    {
      res = myBS.get(spaltenName);
      return res;
    }
    return null;
  }

  @Override
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
  {
    if (hasBackingStore())
      return myLOS.get(columnName) != null;
    else
      return true;
  }

  @Override
  public void set(String columnName, String newValue)
      throws ColumnNotFoundException
  {
    if (!isFromLOS())
      throw new UnsupportedOperationException(
        L.m("Nur Datensätze aus dem LOS können manipuliert werden"));
    if (newValue == null)
      throw new IllegalArgumentException(L.m("Override kann nicht null sein"));
    myLOS.put(columnName, newValue);
  }

  @Override
  public void discardLocalOverride(String columnName)
      throws ColumnNotFoundException, NoBackingStoreException
  {
    if (!isFromLOS()){
      return;
    }
    if (!hasBackingStore())
      throw new NoBackingStoreException(
        L.m("Datensatz nicht mit Hintergrundspeicher verknüpft"));
    myLOS.remove(columnName);
  }

  @Override
  public boolean isFromLOS()
  {
    return myLOS != null;
  }

  @Override
  public boolean hasBackingStore()
  {
    return myBS != null;
  }
}
