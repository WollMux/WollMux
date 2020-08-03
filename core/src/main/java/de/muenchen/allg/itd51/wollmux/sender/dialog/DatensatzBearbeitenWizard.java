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
package de.muenchen.allg.itd51.wollmux.sender.dialog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.WindowEvent;
import com.sun.star.ui.dialogs.Wizard;
import com.sun.star.ui.dialogs.WizardButton;
import com.sun.star.ui.dialogs.XWizard;
import com.sun.star.ui.dialogs.XWizardController;
import com.sun.star.ui.dialogs.XWizardPage;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.sender.Sender;
import de.muenchen.allg.dialog.adapter.AbstractWindowListener;

/**
 * Wizard for modifying data sets.
 */
public class DatensatzBearbeitenWizard implements XWizardController
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DatensatzBearbeitenWizard.class);
  private static final int PAGE_COUNT = 3;
  private XWizardPage[] pages = new XWizardPage[PAGE_COUNT];
  private Sender dataset;
  private List<String> dbSchema;
  private XWizard wizard;
  private boolean initSize = true;
  private int sizeChanged = 0;
  private Rectangle lastRectangle;

  private static final short[] paths = { 0, 1, 2 };

  private enum PAGE_ID
  {
    PERSON,
    ORGA,
    FUSSZEILE
  }

  private String[] title = { "Person", "Orga", "Fusszeile" };

  /**
   * A wizard for modifying the data set.
   *
   * @param dataset
   *          The data set to modify.
   * @param dbSchema
   *          The schema of the data set.
   */
  public DatensatzBearbeitenWizard(Sender dataset, List<String> dbSchema)
  {
    this.dataset = dataset;
    this.dbSchema = dbSchema;
  }

  @Override
  public boolean canAdvance()
  {
    return true;
  }

  @Override
  public boolean confirmFinish()
  {
    return true;
  }

  @Override
  public XWizardPage createPage(XWindow parentWindow, short pageId)
  {
    LOGGER.debug("createPage");
    XWizardPage page = null;
    try
    {
      switch (getPageId(pageId))
      {
      case PERSON:
        page = new DatensatzBearbeitenWizardPage(pageId, parentWindow, "DatensatzBearbeitenPerson", dataset, dbSchema);
        break;
        
      case ORGA:
        page = new DatensatzBearbeitenWizardPage(pageId, parentWindow, "DatensatzBearbeitenOrga", dataset, dbSchema);
        break;
        
      case FUSSZEILE:
        page = new DatensatzBearbeitenWizardPage(pageId, parentWindow, "DatensatzBearbeitenFusszeile", dataset,
            dbSchema);
        break;
      }
      pages[pageId] = page;
    } catch (Exception ex)
    {
      LOGGER.error("Page {} konnte nicht erstellt werden", pageId);
      LOGGER.error("", ex);
    }
    return page;
  }

  @Override
  public String getPageTitle(short arg0)
  {
    return title[arg0];
  }

  @Override
  public void onActivatePage(short arg0)
  {
    pages[arg0].activatePage();
    wizard.updateTravelUI();
    wizard.enableButton(WizardButton.HELP, false);
    if(sizeChanged==0)
    {
      wizard.getDialogWindow().setPosSize(0, 0, 900, 550, PosSize.SIZE);
      lastRectangle = wizard.getDialogWindow().getPosSize();
    }
    else
    {
      wizard.getDialogWindow().setPosSize(0, 0, lastRectangle.Width, lastRectangle.Height, PosSize.SIZE);
    }
    // No need for this with LO 6.1, as the dialog is not resizable in 6.1, but in LO 6.4
    if(initSize)
    {
      AbstractWindowListener windowAdapter = new AbstractWindowListener()
      {
        @Override
        public void windowResized(WindowEvent e)
        {
          sizeChanged ++;
          if(sizeChanged>2)
            lastRectangle = wizard.getDialogWindow().getPosSize();
          else
            wizard.getDialogWindow().setPosSize(0, 0, lastRectangle.Width, lastRectangle.Height, PosSize.SIZE);
        }
      };
      wizard.getDialogWindow().addWindowListener(windowAdapter);
      initSize=false;
    }
  }

  @Override
  public void onDeactivatePage(short arg0)
  {
    //not used
  }

  private PAGE_ID getPageId(short pageId)
  {
    return PAGE_ID.values()[pageId];
  }

  /**
   * Show the wizard.
   *
   * @return The result of the wizard.
   * @see XWizard#execute()
   */
  public short execute()
  {
    wizard = Wizard.createSinglePathWizard(UNO.defaultContext, paths, this);
    wizard.setTitle("Datensatz bearbeiten");

    return wizard.execute();
  }

}
