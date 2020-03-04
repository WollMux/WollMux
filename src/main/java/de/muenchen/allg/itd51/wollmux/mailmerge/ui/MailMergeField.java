package de.muenchen.allg.itd51.wollmux.mailmerge.ui;

import java.util.Arrays;

import com.sun.star.awt.XComboBox;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;

/**
 * A wrapper for adding mail merge fields to a {@link XComboBox}.
 */
public class MailMergeField
{
  private final XComboBox comboBox;

  /**
   * Create a new XComboBox with mail merge fields.
   *
   * @param comboBox
   *          The underlying {@link XComboBox}.
   */
  public MailMergeField(XComboBox comboBox)
  {
    this.comboBox = comboBox;
    comboBox.addItem("Bitte wählen..", (short) 0);
  }

  /**
   * Replace all items of the {@link XComboBox} with the fields of the {@link DatasourceModel}. The
   * first entry is selected.
   *
   * @param ds
   *          The current data source.
   */
  public void setMailMergeDatasource(DatasourceModel ds)
  {
    comboBox.removeItems((short) 1, comboBox.getItemCount());
    try
    {
      String[] mailMergeColumnNames = ds.getColumnNames().toArray(new String[] {});
      Arrays.sort(mailMergeColumnNames);
      comboBox.addItems(mailMergeColumnNames, (short) 1);
    } catch (NoTableSelectedException e)
    {
      // nothing to do
    }
    UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
  }
}
