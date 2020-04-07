package de.muenchen.allg.itd51.wollmux.slv;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.chaosfirebolt.converter.RomanInteger;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.util.UnoProperty;

/**
 * A content based directive line. This class provides utility functions for modifying the
 * underlying text range.
 */
public class ContentBasedDirectiveItem
{

  private static final String OF = " von ";

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectiveItem.class);

  private XTextRange textRange;

  /**
   * A new content based directive.
   *
   * @param textRange
   *          The underlying text range.
   */
  public ContentBasedDirectiveItem(XTextRange textRange)
  {
    this.textRange = textRange;
  }

  public XTextRange getTextRange()
  {
    return textRange;
  }

  /**
   * Test whether this is a copy.
   *
   * @return true, if it is a copy.
   */
  public boolean isCopy()
  {
    String str = Utils.getStringOfXTextRange(textRange);
    return str.contains(
        ContentBasedDirectiveConfig.getName() + OF + ContentBasedDirectiveConfig.getNumber(1));
  }

  /**
   * Test whether this is a content based directive with a recipient line.
   *
   * @return true, if it is a content based directive with a recipient line.
   */
  public boolean isItemWithRecipient()
  {
    String paraStyleName = getParagraphStyleName();
    return paraStyleName.startsWith(ContentBasedDirectiveModel.PARA_STYLE_NAME_CBD_WITH_RECIPIENT);
  }

  /**
   * Test whether this is a content based directive.
   *
   * @return true, if it is a content based directive.
   */
  public boolean isItem()
  {
    String paraStyleName = getParagraphStyleName();
    return paraStyleName.startsWith(ContentBasedDirectiveModel.PARA_STYLE_NAME_CBD);
  }

  /**
   * Test whether this is a recipient line.
   *
   * @return true, if it is a recipient line.
   */
  public boolean isRecipientLine()
  {
    String paraStyleName = getParagraphStyleName();
    return paraStyleName.startsWith(ContentBasedDirectiveModel.PARA_STYLE_NAME_RECIPIENT);
  }

  /**
   * Format as {@link ContentBasedDirectiveModel#PARA_STYLE_NAME_DEFAULT}.
   */
  public void formatDefault()
  {
    Utils.setProperty(textRange, UnoProperty.PARA_STYLE_NAME,
        ContentBasedDirectiveModel.PARA_STYLE_NAME_DEFAULT);
    formatNumber();
  }

  /**
   * Format as {@link ContentBasedDirectiveModel#PARA_STYLE_NAME_COPY}.
   */
  public void formatCopy()
  {
    Utils.setProperty(textRange, UnoProperty.PARA_STYLE_NAME,
        ContentBasedDirectiveModel.PARA_STYLE_NAME_COPY);
    formatNumber();
  }

  /**
   * Format as {@link ContentBasedDirectiveModel#PARA_STYLE_NAME_CBD}.
   */
  public void formatItem()
  {
    Utils.setProperty(textRange, UnoProperty.PARA_STYLE_NAME,
        ContentBasedDirectiveModel.PARA_STYLE_NAME_CBD);
    formatNumber();
  }

  /**
   * Format as {@link ContentBasedDirectiveModel#PARA_STYLE_NAME_RECIPIENT}.
   */
  public void formatRecipientLine()
  {
    Utils.setProperty(textRange, UnoProperty.PARA_STYLE_NAME,
        ContentBasedDirectiveModel.PARA_STYLE_NAME_RECIPIENT);
    formatNumber();
  }

  /**
   * Format as {@link ContentBasedDirectiveModel#PARA_STYLE_NAME_CBD_WITH_RECIPIENT}.
   */
  public void formatVerfuegungspunktWithZuleitung()
  {
    Utils.setProperty(textRange,
        UnoProperty.PARA_STYLE_NAME,
        ContentBasedDirectiveModel.PARA_STYLE_NAME_CBD_WITH_RECIPIENT);
    formatNumber();
  }

  /**
   * Format the number with {@link ContentBasedDirectiveModel#CHAR_STYLE_NAME_NUMBER}.
   */
  public void formatNumber()
  {
    XTextCursor zifferOnly = getZifferOnly();
    if (zifferOnly != null)
    {
      Utils.setProperty(zifferOnly,
          UnoProperty.CHAR_STYLE_NAME,
          ContentBasedDirectiveModel.CHAR_STYLE_NAME_NUMBER);

      // Zeichen danach auf Standardformatierung setzen, damit der Text, der
      // danach geschrieben wird nicht auch obiges Zeichenformat besitzt:
      // ("Standard" gilt laut DevGuide auch in englischen Versionen)
      Utils.setProperty(zifferOnly.getEnd(), UnoProperty.CHAR_STYLE_NAME, "Standard");
    }
  }

  /**
   * Deletes this content based directive. This means the format is reseted and any number is
   * removed. If it is a copy, the text is also deleted.
   */
  public void remove()
  {
    formatDefault();

    XTextCursor zifferOnly = getZifferOnly();
    if (zifferOnly != null)
    {
      zifferOnly.setString("");
    }

    // if it is a copy, delete all
    if (isCopy())
    {
      textRange.setString("");

      XParagraphCursor parDeleter = UNO
          .XParagraphCursor(textRange.getText().createTextCursorByRange(textRange.getEnd()));

      // remove spaces before and after
      parDeleter.goLeft((short) 1, true);
      parDeleter.setString("");
      if (parDeleter.goRight((short) 1, false) && parDeleter.isEndOfParagraph())
      {
        parDeleter.goLeft((short) 1, true);
        parDeleter.setString("");
      }
    }
  }

  /**
   * Get the text range containing the number and a following tab.
   *
   * @return The text range with the number and tab or null with there is no number.
   */
  public XTextCursor getZifferOnly()
  {
    String text = textRange.getString();
    int index = text.trim().indexOf('.');
    int start = text.indexOf(text.trim());
    if (index < 0)
    {
      return null;
    } else
    {
      XParagraphCursor cursor = UNO
          .XParagraphCursor(textRange.getText().createTextCursorByRange(textRange.getStart()));
      cursor.goRight((short) start, false);
      cursor.goRight((short) index, true);

      try
      {
        // Test if we found a number
        RomanInteger.parse(cursor.getString());
        short goRight = 2; // . and tab
        cursor.goRight(goRight, true);
        return cursor;
      } catch (IllegalArgumentException e)
      {
        return null;
      }
    }
  }

  /**
   * Gets the part of a copy line after the numbers, which was manually added.
   *
   * @return The suffix of the copy line. Can be the empty string.
   */
  public String getCopySuffix()
  {
    String str = textRange.getString();
    Matcher m = Pattern.compile("[XIV0-9]+\\.\\s*" + ContentBasedDirectiveConfig.getName() + OF
        + ContentBasedDirectiveConfig.getNumber(1) + "(, [XIV0-9]+\\.)*( und [XIV0-9]+\\.)?(.*)")
        .matcher(str);
    if (m.matches())
      return m.group(3);
    return "";
  }

  /**
   * Adopt the number of a content based directive.
   *
   * @param count
   *          The new number.
   */
  public void adoptNumber(int count)
  {
    if (isCopy())
    {
      // Behandlung von Paragraphen mit einem "Abdruck"-String
      String abdruckStr = copyString(count) + getCopySuffix();
      if (!textRange.getString().equals(abdruckStr))
      {
        getTextRange().setString(abdruckStr);
        formatNumber();
      }
    } else
    {
      // Behandlung von normalen Verfügungspunkten:
      String numberStr = ContentBasedDirectiveConfig.getNumber(count) + "\t";
      XTextRange zifferOnly = getZifferOnly();
      if (zifferOnly != null)
      {
        zifferOnly.setString(numberStr);
      } else
      {
        // Nummer neu anlegen, wenn sie noch gar nicht existierte
        zifferOnly = textRange.getText().createTextCursorByRange(textRange.getStart());
        zifferOnly.setString(numberStr);
        formatNumber();
      }
    }
  }

  /**
   * Returns the content of lines associated with this item.
   *
   * @return The content.
   */
  public String getFullLinesOfSelectedCopyLines()
  {
    StringBuilder fullText = new StringBuilder("");
    UnoCollection<XTextRange> texts = UnoCollection.getCollection(textRange, XTextRange.class);
    if (texts != null)
    {
      for (XTextRange text : texts)
      {
        fullText.append(Utils.getStringOfXTextRange(text));
      }
    }
    return fullText.toString();
  }

  /**
   * Creates a copy line for the given number.
   *
   * @param number
   *          The number of the item.
   * @return The copy line content.
   */
  public static String copyString(int number)
  {
    StringBuilder str = new StringBuilder();
    str.append(ContentBasedDirectiveConfig.getNumber(number) + "\t");
    str.append(ContentBasedDirectiveConfig.getName() + OF);
    str.append(ContentBasedDirectiveConfig.getNumber(1));
    for (int j = 2; j < (number - 1); ++j)
    {
      str.append(", " + ContentBasedDirectiveConfig.getNumber(j));
    }
    if (number >= 3)
    {
      str.append(" und " + ContentBasedDirectiveConfig.getNumber(number - 1));
    }
    return str.toString();
  }

  /**
   * The style of the current text range.
   *
   * @return The name of the style or an empty string.
   */
  private String getParagraphStyleName()
  {
    String paraStyleName = "";
    try
    {
      paraStyleName = AnyConverter
          .toString(Utils.getProperty(textRange, UnoProperty.PARA_STYLE_NAME));
    } catch (IllegalArgumentException e)
    {
      LOGGER.trace("", e);
    }
    return paraStyleName;
  }
}
