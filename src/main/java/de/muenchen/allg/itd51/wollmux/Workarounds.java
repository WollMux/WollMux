/*
 * Dateiname: Workarounds.java
 * Projekt  : WollMux
 * Funktion : Referenziert alle temporären Workarounds an einer zentralen Stelle
 *
 * Copyright (c) 2009-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 01.04.2009 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 * @version 1.0
 *
 */package de.muenchen.allg.itd51.wollmux;

import java.awt.Toolkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Diese Klasse referenziert alle temporären Workarounds, die im WollMux aufgenommen
 * wurden, an einer zentralen Stelle. Sie definiert Methoden, die die Steuerung
 * übernehmen, ob ein Workaround anzuwenden ist oder nicht.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class Workarounds
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(Workarounds.class);

  private static Boolean workaround73229 = null;

  private static Boolean workaroundWMClass = null;

  public static Boolean applyWorkaround(String issueNumber)
  {
    LOGGER.debug("Workaround für Issue "
      + issueNumber
      + " aktiv. Bestimmte Features sind evtl. nicht verfügbar. Die Performance kann ebenfalls leiden.");
    return Boolean.TRUE;
  }

  /**
   * Beim Einfügen eines Dokuments mit einer Section und einem Seitenumbruch in ein
   * anderes wird eine leere Seite am Anfang eingefügt.
   *
   * Issue #73229 betrifft den WollMux-Seriendruck in ein Gesamtdokument und ist
   * aktuell für OOo Later priorisiert - wird also nicht in absehbarer Zeit behoben
   * sein.
   */
  public static boolean applyWorkaroundForOOoIssue73229()
  {
    if (workaround73229 == null)
    {
      workaround73229 = applyWorkaround("73229");
    }
    return workaround73229.booleanValue();
  }

  /**
   * Unter Linux kann kein Schnellstarter erstellt werden, da
   * <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6528430">Issue 6528430</a>. Damit
   * es funktioniert, muss das Attribut WM_Class gesetzt werden, da dann eine korrekte Verknüpfung
   * zwischen dem Schnellstarter und der .desktop Datei entsteht.
   *
   * @return true wenn WM_Class gesetzt wurde.
   */
  public static boolean applyWorkaroundForWMClass()
  {
    if (workaroundWMClass == null)
    {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Class<?> xtoolkit = toolkit.getClass();
      if ("sun.awt.X11.XToolkit".equals(xtoolkit.getName()))
      {
        try
        {
          java.lang.reflect.Field awtAppClassNameField = xtoolkit.getDeclaredField("awtAppClassName");
          awtAppClassNameField.setAccessible(true);
          awtAppClassNameField.set(null, "WollMux");
          workaroundWMClass = applyWorkaround("WMClass");
        } catch (Exception e) {
          LOGGER.error(L.m("WMClass konnte nicht gesetzt werden."), e);
          workaroundWMClass = Boolean.FALSE;
        }
      } else {
        workaroundWMClass = Boolean.FALSE;
      }
    }
    return workaroundWMClass.booleanValue();
  }

}
