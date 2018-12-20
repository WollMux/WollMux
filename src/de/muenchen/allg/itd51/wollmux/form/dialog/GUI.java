package de.muenchen.allg.itd51.wollmux.form.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementContext;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementEventHandler;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Textarea;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.UIElement;
import de.muenchen.allg.itd51.wollmux.core.form.model.Control;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormValueChangedListener;
import de.muenchen.allg.itd51.wollmux.core.form.model.Tab;
import de.muenchen.allg.itd51.wollmux.core.form.model.VisibilityChangedListener;
import de.muenchen.allg.itd51.wollmux.core.form.model.VisibilityGroup;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;

public class GUI
    implements UIElementEventHandler, FormValueChangedListener, VisibilityChangedListener
{

  private static final Logger LOGGER = LoggerFactory.getLogger(GUI.class);

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private static final int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private static final int BUTTON_BORDER = 2;

  /**
   * GridBagLayout hat eine Begrenzung auf maximal 512 Grid-Elemente pro Koordinatenrichtung,
   * deshalb wird verhindert, dass zuviele Elemente auf einem Tab eingefügt werden, indem maximal
   * soviele wie hier angegeben angezeigt werden. Der Wert hier ist etwas weniger als 512, um Puffer
   * zu bieten für das Hinzufügen von Elementen, die immer vorhanden sein sollen.
   */
  private static final int GRID_MAX = 500;

  /**
   * Die Farbe mit der Felder normalerweise eingefärbt sind. Gegenstück zu
   * {@link #plausiMarkerColor}.
   */
  private Color normalColor = new JTextField().getBackground();

  private Color plausiColor;

  private Rectangle formGUIBounds;

  private JFrame myFrame;

  private Rectangle naturalFrameBounds;

  private Insets windowInsets;

  private Rectangle maxWindowBounds;

  private WindowPosSizeSetter windowPosSizeSetter = new WindowPosSizeSetter();

  private UIElementFactory uiElementFactory;

  private UIElementContext panelContext;

  private UIElementContext buttonContext;

  private FormController controller;

  private JTabbedPane myTabbedPane;

  private Map<String, UIElement> uiElements = new HashMap<>();

  private Map<String, List<UIElement>> visibilityGroups = new HashMap<>();

  private WindowAdapter windowAdapter = new WindowAdapter()
  {
    @Override
    public void windowClosing(WindowEvent e)
    {
      controller.close();
      dispose();
    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {
      arrangeWindows();
    }
  };

  private ComponentAdapter componentAdapter = new ComponentAdapter()
  {

    @Override
    public void componentResized(ComponentEvent e)
    {
      arrangeWindows();
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
      arrangeWindows();
    }
  };

  /**
   * Solange dieses Flag false ist, werden Events von UI Elementen ignoriert.
   */
  private boolean processUIElementEvents = false;

  public GUI(FormController controller, ConfigThingy formFensterConf)
  {
    this.controller = controller;
    formGUIBounds = Common.parseDimensions(formFensterConf);
  }

  public void create(FormModel model, boolean visible)
  {
    Common.setLookAndFeelOnce();
    initFactories();
    plausiColor = model.getPlausiMarkerColor();

    // Create and set up the window.
    myFrame = new JFrame(model.getTitle());
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(windowAdapter);
    myFrame.addComponentListener(componentAdapter);

    // WollMux-Icon für das FormGUI-Fenster
    Common.setWollMuxIcon(myFrame);

    myTabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    boolean focus = true;
    for (Map.Entry<String, Tab> entry : model.getTabs().entrySet())
    {
      Tab tab = entry.getValue();
      myTabbedPane.addTab(tab.getTitle(), null, createTab(tab, model.getPlausiMarkerColor(), focus),
          tab.getTip());
      focus = false;
    }

    myFrame.getContentPane().add(myTabbedPane);

    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();

    myFrame.pack();
    myFrame.setResizable(true);
    myFrame.setVisible(visible);

    naturalFrameBounds = myFrame.getBounds();

    // Bestimmen der Breite des Fensterrahmens.
    windowInsets = myFrame.getInsets();

    setFormGUISizeAndLocation();
    arrangeWindows();

    processUIElementEvents = true;
  }

  private Component createTab(Tab tab, Color plausiMarkerColor, boolean focus)
  {
    JPanel myPanel = new JPanel(new GridBagLayout());
    JPanel mainPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbcMainPanel = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
        GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    JScrollPane scrollPane = new JScrollPane(mainPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getVerticalScrollBar().setUnitIncrement(Common.getVerticalScrollbarUnitIncrement());
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
    myPanel.add(scrollPane, gbcMainPanel);

    int y = 0;
    boolean hasFocusable = false;

    for (Control control : tab.getControls())
    {
      UIElement uiElement = uiElementFactory.createUIElement(panelContext, control);
      // Dem ersten Element den Focus geben, wenn dies gewünscht ist.
      if (focus)
      {
        SwingUtilities.invokeLater(uiElement::takeFocus);
        focus = false;
      }
      uiElements.put(control.getId(), uiElement);
      boolean visible = true;
      for (VisibilityGroup group : control.getGroups())
      {
        visible = visible && group.isVisible();
        if (!visibilityGroups.containsKey(group.getGroupId()))
        {
          visibilityGroups.put(group.getGroupId(), new ArrayList<>(1));
          group.addVisibilityChangedListener(this);
        }
        visibilityGroups.get(group.getGroupId()).add(uiElement);
      }
      addFocusListener(uiElement);
      /**************************************************************************
       * UI Element und evtl. vorhandenes Zusatzlabel zum GUI hinzufügen.
       *************************************************************************/
      int compoX = 0;
      int compoWidthIncrement = 0;
      if (!uiElement.getLabelType().equals(UIElement.LabelPosition.NONE))
      {
        Component label = uiElement.getLabel();
        int labelX = 0;
        boolean labelIsEmpty = false;
        if (uiElement.getLabelType().equals(UIElement.LabelPosition.LEFT))
        {
          compoX = 1;
          try
          {
            labelIsEmpty = ((JLabel) label).getText().length() == 0;
            if (labelIsEmpty)
            {
              compoWidthIncrement = 1;
              compoX = 0;
            }
          } catch (Exception x)
          {
          }
        } else
          labelX = 1;

        if (label != null && !labelIsEmpty)
        {
          GridBagConstraints gbc = (GridBagConstraints) uiElement.getLabelLayoutConstraints();
          gbc.gridx = labelX;
          gbc.gridy = y;
          mainPanel.add(label, gbc);
        }
      }
      GridBagConstraints gbc = (GridBagConstraints) uiElement.getLayoutConstraints();
      gbc.gridx = compoX;
      gbc.gridwidth += compoWidthIncrement; // wird nachher wieder abgezogen weil Objekt shared ist
      gbc.gridy = y;
      ++y;
      Component component = uiElement.getComponent();
      hasFocusable |= component.isFocusable();
      mainPanel.add(component, gbc);
      gbc.gridwidth -= compoWidthIncrement; // wieder abziehen, weil Objekt ja shared ist

      uiElement.setString(control.getValue());
      uiElement.setVisible(visible);
      if (!control.isOkay())
      {
        uiElement.setBackground(plausiMarkerColor);
      }
      control.addFormModelChangedListener(this);

      if (y > GRID_MAX)
        break;
    }

    if (y > GRID_MAX)
      LOGGER.error(L.m("Zu viele Formularelemente auf einem Tab => nicht alle werden angezeigt"));

    if (!hasFocusable)
    {
      GridBagConstraints gbc = new GridBagConstraints(0, y, 0, 0, 0.0, 0.0,
          GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
      final JPanel tabBlocker = new JPanel();
      tabBlocker.setSize(0, 0);
      tabBlocker.setFocusable(true);
      tabBlocker.setOpaque(false);
      tabBlocker.setRequestFocusEnabled(true);
      mainPanel.add(tabBlocker, gbc);
    }
    /******************************************************************************
     * Für die Buttons ein eigenes Panel anlegen und mit UIElementen befüllen.
     *****************************************************************************/
    GridBagConstraints gbcButtonPanel = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    gbcButtonPanel.gridx = 0;
    gbcButtonPanel.gridy = y;
    myPanel.add(createButtonPanel(tab.getButtons()), gbcButtonPanel);

    return myPanel;
  }

  private JComponent createButtonPanel(List<Control> buttons)
  {
    JPanel buttonPanel = new JPanel(new GridBagLayout());

    addTabSwitcher(buttonPanel);
    int x = 0;

    for (Control control : buttons)
    {
      UIElement uiElement = uiElementFactory.createUIElement(buttonContext, control);
      uiElements.put(control.getId(), uiElement);
      boolean visible = true;
      for (VisibilityGroup group : control.getGroups())
      {
        visible = visible && group.isVisible();
        if (!visibilityGroups.containsKey(group.getGroupId()))
        {
          visibilityGroups.put(group.getGroupId(), new ArrayList<>(1));
          group.addVisibilityChangedListener(this);
        }
        visibilityGroups.get(group.getGroupId()).add(uiElement);
      }
      uiElement.setEnabled(visible);
      control.addFormModelChangedListener(this);
      int compoX = x;
      if (!uiElement.getLabelType().equals(UIElement.LabelPosition.NONE))
      {
        int labelX = x;
        ++x;
        if (uiElement.getLabelType().equals(UIElement.LabelPosition.LEFT))
          compoX = x;
        else
          labelX = x;

        Component label = uiElement.getLabel();
        if (label != null)
        {
          GridBagConstraints gbc = (GridBagConstraints) uiElement.getLabelLayoutConstraints();
          gbc.gridx = labelX;
          gbc.gridy = 0;
          buttonPanel.add(label, gbc);
        }
      }

      GridBagConstraints gbc = (GridBagConstraints) uiElement.getLayoutConstraints();
      gbc.gridx = compoX;
      gbc.gridy = 0;
      ++x;
      buttonPanel.add(uiElement.getComponent(), gbc);

      if (x > GRID_MAX)
        break;
    }
    if (x > GRID_MAX)
      LOGGER.error(L.m("Zu viele Buttons auf einem Tab => nicht alle werden angezeigt"));
    return buttonPanel;
  }

  /**
   * Fügt buttonPanel (muss ein GridBagLayout verwenden) an Index 0 eine unsichtbare Komponente
   * hinzu, die wenn sie den Fokus bekommt (was nur über die TAB-Taste geschehen kann) und zwar von
   * einer anderen Komponente als der 2,Komponente des buttonPanels, einen action "nextTab" Event
   * absetzt, falls es noch ein auf das aktuelle Tab folgendes Tab gibt, das aktiv ist. Falls es
   * kein passendes Tab gibt, bekommt die letzte Komponente von buttonPanel den Fokus.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addTabSwitcher(final JPanel buttonPanel)
  {
    GridBagConstraints gbc = new GridBagConstraints(0, 0, 0, 0, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    final JPanel tabSwitcherCompo = new JPanel();
    tabSwitcherCompo.setSize(1, 1);
    tabSwitcherCompo.setFocusable(true);
    tabSwitcherCompo.setOpaque(false);
    tabSwitcherCompo.setFocusable(true);
    tabSwitcherCompo.setRequestFocusEnabled(false);
    tabSwitcherCompo.addFocusListener(new FocusAdapter()
    {

      @Override
      public void focusGained(FocusEvent e)
      {
        if (buttonPanel.getComponentCount() > 1
            && e.getOppositeComponent() == buttonPanel.getComponent(1))
        {
          tabSwitcherCompo.transferFocusBackward();
        } else
        {
          int startIdx = myTabbedPane.getSelectedIndex();
          int idx = startIdx;
          do
          {
            ++idx;
            if (idx >= myTabbedPane.getTabCount())
            {
              idx = -1;
              break;
            }
            if (myTabbedPane.isEnabledAt(idx))
              break;
          } while (idx != startIdx);
          if (idx > -1)
            processUiElementEvent(null, "action", new String[] { "nextTab" });
          else
            buttonPanel.getComponent(buttonPanel.getComponentCount() - 1).requestFocusInWindow();
        }
      }
    });

    buttonPanel.add(tabSwitcherCompo, gbc, 0);
  }

  /**
   * Fügt einem Steuerelement einen FocusListener hinzu, der dafür sorgt, daß der Container zu dem
   * Element hinscrollt, sobald es den Fokus erhält.
   *
   * @param uiElement
   *          das Steuerelement, das überwacht werden soll.
   * @author Andor Ertsey (D-III-ITD-D101)
   */
  private void addFocusListener(UIElement uiElement)
  {
    Component c = uiElement.getComponent();
    if (uiElement instanceof Textarea)
      c = ((Textarea) uiElement).getTextArea();

    c.addFocusListener(new FocusAdapter()
    {
      @Override
      public void focusGained(FocusEvent e)
      {
        Container c = e.getComponent().getParent();
        if (e.getComponent() instanceof JTextArea)
          c = c.getParent();

        if (c != null && c instanceof JComponent)
        {
          java.awt.Rectangle b = e.getComponent().getBounds();
          b.x = 0;
          ((JComponent) c).scrollRectToVisible(b);
          c.repaint();
        }
      }
    });
  }

  /**
   * Initialisiert die UIElementFactory, die zur Erzeugung der UIElements verwendet wird.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void initFactories()
  {
    Map<String, GridBagConstraints> mapTypeToLayoutConstraints = new HashMap<>();
    Map<String, UIElement.LabelPosition> mapTypeToLabelType = new HashMap<>();
    Map<String, GridBagConstraints> mapTypeToLabelLayoutConstraints = new HashMap<>();

    // int gridx, int gridy, int gridwidth, int gridheight, double weightx,
    // double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcCombobox = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcTextarea = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcCheckbox = new GridBagConstraints(0, 0, 2/* JA */, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcLabel = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    GridBagConstraints gbcHsep = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(3 * TF_BORDER, 0, 2 * TF_BORDER, 0), 0, 0);
    GridBagConstraints gbcVsep = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
        new Insets(0, TF_BORDER, 0, TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
        GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    mapTypeToLayoutConstraints.put("default", gbcTextfield);
    mapTypeToLabelType.put("default", UIElement.LabelPosition.LEFT);
    mapTypeToLabelLayoutConstraints.put("default", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("textfield", gbcTextfield);
    mapTypeToLabelType.put("textfield", UIElement.LabelPosition.LEFT);
    mapTypeToLabelLayoutConstraints.put("textfield", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
    mapTypeToLabelType.put("combobox", UIElement.LabelPosition.LEFT);
    mapTypeToLabelLayoutConstraints.put("combobox", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
    mapTypeToLabelType.put("h-glue", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("h-glue", null);
    mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
    mapTypeToLabelType.put("v-glue", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("v-glue", null);

    mapTypeToLayoutConstraints.put("textarea", gbcTextarea);
    mapTypeToLabelType.put("textarea", UIElement.LabelPosition.LEFT);
    mapTypeToLabelLayoutConstraints.put("textarea", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("label", gbcLabel);
    mapTypeToLabelType.put("label", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("label", null);

    mapTypeToLayoutConstraints.put("checkbox", gbcCheckbox);
    mapTypeToLabelType.put("checkbox", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("checkbox", null); // hat label
                                                           // integriert

    mapTypeToLayoutConstraints.put("button", gbcButton);
    mapTypeToLabelType.put("button", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("button", null);

    mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
    mapTypeToLabelType.put("h-separator", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("h-separator", null);
    mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
    mapTypeToLabelType.put("v-separator", UIElement.LabelPosition.NONE);
    mapTypeToLabelLayoutConstraints.put("v-separator", null);

    panelContext = new UIElementContext();
    panelContext.setMapTypeToLabelLayoutConstraints(mapTypeToLabelLayoutConstraints);
    panelContext.setMapTypeToLabelType(mapTypeToLabelType);
    panelContext.setMapTypeToLayoutConstraints(mapTypeToLayoutConstraints);
    panelContext.setUiElementEventHandler(this);
    Map<String, String> panelMapTypeToType = new HashMap<>();
    panelMapTypeToType.put("separator", "h-separator");
    panelMapTypeToType.put("glue", "v-glue");
    panelContext.setMapTypeToType(panelMapTypeToType);

    buttonContext = new UIElementContext();
    buttonContext.setMapTypeToLabelLayoutConstraints(mapTypeToLabelLayoutConstraints);
    buttonContext.setMapTypeToLabelType(mapTypeToLabelType);
    buttonContext.setMapTypeToLayoutConstraints(mapTypeToLayoutConstraints);
    buttonContext.setUiElementEventHandler(this);
    Map<String, String> buttonMapTypeToType = new HashMap<>();
    buttonMapTypeToType.put("separator", "v-separator");
    buttonMapTypeToType.put("glue", "h-glue");
    buttonContext.setMapTypeToType(buttonMapTypeToType);

    Set<String> supportedActions = new HashSet<>();
    supportedActions.add("abort");
    supportedActions.add("nextTab");
    supportedActions.add("prevTab");
    supportedActions.add("funcDialog");
    supportedActions.add("form2PDF");
    supportedActions.add("save");
    supportedActions.add("saveAs");
    supportedActions.add("printForm");
    supportedActions.add("closeAndOpenExt");
    supportedActions.add("saveTempAndOpenExt");
    supportedActions.add("openTemplate");
    supportedActions.add("openExt");
    supportedActions.add("form2EMail");
    panelContext.setSupportedActions(supportedActions);
    buttonContext.setSupportedActions(supportedActions);

    uiElementFactory = new UIElementFactory();

  }

  /**
   * Setzt Größe und Ort der FormGUI.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void setFormGUISizeAndLocation()
  {
    Rectangle frameBounds = new Rectangle(naturalFrameBounds);
    LOGGER.debug("setFormGUISizeAndLocation: frameBounds=" + frameBounds);

    switch (formGUIBounds.width)
    {
    case Common.DIMENSION_UNSPECIFIED: // natural width
      if (frameBounds.width > (0.66 * maxWindowBounds.width))
        frameBounds.width = (int) (0.66 * maxWindowBounds.width);
      break;
    case Common.DIMENSION_MAX: // max
      frameBounds.width = maxWindowBounds.width;
      break;
    default: // specified width
      frameBounds.width = formGUIBounds.width;
      break;
    }

    switch (formGUIBounds.height)
    {
    case Common.DIMENSION_UNSPECIFIED: // natural height
      break;
    case Common.DIMENSION_MAX: // max
      frameBounds.height = maxWindowBounds.height;
      break;
    default: // specified height
      frameBounds.height = formGUIBounds.height;
      break;
    }

    switch (formGUIBounds.x)
    {
    case Common.COORDINATE_CENTER: // center
      frameBounds.x = maxWindowBounds.x + (maxWindowBounds.width - frameBounds.width) / 2;
      break;
    case Common.COORDINATE_MAX: // max
      frameBounds.x = maxWindowBounds.x + maxWindowBounds.width - frameBounds.width;
      break;
    case Common.COORDINATE_MIN: // min
      frameBounds.x = maxWindowBounds.x;
      break;
    case Common.COORDINATE_UNSPECIFIED: // kein Wert angegeben
      frameBounds.x = maxWindowBounds.x;
      break;
    default: // Wert angegeben, wird nur einmal berücksichtigt.
      frameBounds.x = formGUIBounds.x;
      formGUIBounds.x = Common.COORDINATE_UNSPECIFIED;
      break;
    }

    switch (formGUIBounds.y)
    {
    case Common.COORDINATE_CENTER: // center
      frameBounds.y = maxWindowBounds.y + (maxWindowBounds.height - frameBounds.height) / 2;
      break;
    case Common.COORDINATE_MAX: // max
      frameBounds.y = maxWindowBounds.y + maxWindowBounds.height - frameBounds.height;
      break;
    case Common.COORDINATE_MIN: // min
      frameBounds.y = maxWindowBounds.y;
      break;
    case Common.COORDINATE_UNSPECIFIED: // kein Wert angegeben
      frameBounds.y = maxWindowBounds.y;
      break;
    default: // Wert angegeben, wird nur einmal berücksichtigt.
      frameBounds.y = formGUIBounds.y;
      formGUIBounds.y = Common.COORDINATE_UNSPECIFIED;
      break;
    }

    /*
     * Workaround für Bug in Java: Standardmaessig werden die MaximumWindowBounds nicht
     * berücksichtigt beim ersten Layout (jedoch schon, wenn sich die Taskleiste verändert).
     */
    if (frameBounds.y + frameBounds.height > maxWindowBounds.y + maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.y + maxWindowBounds.height - frameBounds.y;

    myFrame.setBounds(frameBounds);
    myFrame.validate(); // ohne diese wurde in Tests manchmal nicht neu gezeichnet
    myFrame.toFront();
  }

  /**
   * Arrangiert das Writer Fenster so, dass es neben dem Formular-Fenster sitzt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void arrangeWindows()
  {
    Rectangle frameBounds = new Rectangle(myFrame.getBounds());
    LOGGER.debug("Maximum window bounds " + maxWindowBounds + "| window insets " + windowInsets
        + "| frame bounds " + frameBounds);

    /*
     * Das Addieren von windowInsets.left und windowInsets.right ist eine Heuristik. Da sich
     * setWindowPosSize() unter Windows und Linux anders verhält, gibt es keine korrekte Methode
     * (die mir bekannt ist), um die richtige Ausrichtung zu berechnen.
     */
    int docX = frameBounds.width + frameBounds.x + windowInsets.left;
    int docWidth = maxWindowBounds.width - frameBounds.width - frameBounds.x - windowInsets.right;
    if (docWidth < 0)
    {
      docX = maxWindowBounds.x;
      docWidth = maxWindowBounds.width;
    }
    int docY = maxWindowBounds.y + windowInsets.top;
    /*
     * Das Subtrahieren von 2*windowInsets.bottom ist ebenfalls eine Heuristik. (siehe weiter oben)
     */
    int docHeight = maxWindowBounds.y + maxWindowBounds.height - docY - 2 * windowInsets.bottom;

    windowPosSizeSetter.setWindowPosSize(docX, docY, docWidth, docHeight);
  }

  private class WindowPosSizeSetter extends Timer implements ActionListener
  {
    private static final long serialVersionUID = 3722895126444827532L;

    private int x;

    private int y;

    private int width;

    private int height;

    public WindowPosSizeSetter()
    {
      super(100, null);
      addActionListener(this);
      setRepeats(false);
      setCoalesce(true);
    }

    public void setWindowPosSize(int x, int y, int width, int height)
    {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      restart();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      controller.setWindowPosSize(x, y, width, height);
    }
  }

  /**
   * Schliesst die FormGUI und alle zugehörigen Fenster.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(() -> {
        try
        {
          myFrame.dispose();
        } catch (Exception x)
        {
        }
      });
    } catch (Exception x)
    {
    }
  }

  public void createGUI(Runnable runner)
  {
    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      if (SwingUtilities.isEventDispatchThread())
      {
        runner.run();
      }
      else
        SwingUtilities.invokeAndWait(runner);
    } catch (InvocationTargetException | InterruptedException x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Die zentrale Anlaufstelle für alle von UIElementen ausgelösten Events (siehe
   * {@link UIElementEventHandler#processUiElementEvent(UIElement, String, Object[])} ).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  @Override
  public void processUiElementEvent(UIElement source, String eventType, Object[] args)
  {
    if (!processUIElementEvents)
    {
      return;
    }
    try
    {
      processUIElementEvents = false;
      if (WollMuxFiles.isDebugMode())
      {
        StringBuilder buffy = new StringBuilder("UIElementEvent: " + eventType + "(");
        for (int i = 0; i < args.length; ++i)
          buffy.append((i == 0 ? "" : ",") + args[i]);
        if (source != null)
          buffy.append(") on UIElement " + source.getId());
        LOGGER.debug(buffy.toString());
      }

      if ("valueChanged".equals(eventType))
      {
        controller.setValue(source.getId(), source.getString(), null);
      } else if ("action".equals(eventType))
      {
        String action = (String) args[0];
        if ("abort".equals(action))
        {
          controller.close();
        } else if ("nextTab".equals(action))
        {
          nextTab();
        } else if ("prevTab".equals(action))
        {
          prevTab();
        } else if ("funcDialog".equals(action))
        {
          String dialogName = (String) args[1];
          controller.openDialog(dialogName);
        } else if ("closeAndOpenExt".equals(action))
        {
          controller.closeAndOpenExt((String) args[1]);
        } else if ("saveTempAndOpenExt".equals(action))
        {
          controller.saveTempAndOpenExt((String) args[1]);
        } else if ("printForm".equals(action))
        {
          controller.print();
        } else if ("form2PDF".equals(action))
        {
          controller.pdf();
        } else if ("save".equals(action))
        {
          controller.save();
        } else if ("saveAs".equals(action))
        {
          controller.saveAs();
        } else if ("openTemplate".equals(action) || "openDocument".equals(action))
        {
          String fragId = (String) args[1];
          List<String> fragIds = new ArrayList<>();
          fragIds.add(fragId);
          controller.openTemplateOrDocument(fragIds);
        } else if ("openExt".equals(action))
        {
          OpenExt openExInstance = OpenExt.getInstance((String) args[1], (String) args[2]);
          openExInstance.launch(x -> LOGGER.error("", x));
        } else if ("form2EMail".equals(action))
        {
          controller.sendAsEmail();
        }
      } else if ("focus".equals(eventType))
      {
        if ("lost".equals(args[0]))
          controller.focusLost(source.getId());
        else
          controller.focusGained(source.getId());
      }
    } catch (Exception x)
    {
      LOGGER.error("", x);
    } finally
    {
      processUIElementEvents = true;
    }
  }

  private void prevTab()
  {
    int startIdx = myTabbedPane.getSelectedIndex();
    int idx = startIdx;
    do
    {
      if (idx == 0)
      {
        idx = myTabbedPane.getTabCount();
      }
      --idx;
      if (myTabbedPane.isEnabledAt(idx))
      {
        break;
      }
    } while (idx != startIdx);

    myTabbedPane.setSelectedIndex(idx);
  }

  private void nextTab()
  {
    int startIdx = myTabbedPane.getSelectedIndex();
    int idx = startIdx;
    do
    {
      ++idx;
      if (idx >= myTabbedPane.getTabCount())
      {
        idx = 0;
      }
      if (myTabbedPane.isEnabledAt(idx))
      {
        break;
      }
    } while (idx != startIdx);

    myTabbedPane.setSelectedIndex(idx);
  }

  @Override
  public void statusChanged(String id, boolean okay)
  {
    SwingUtilities.invokeLater(() -> {
      if (uiElements.containsKey(id))
      {
        if (okay)
        {
          uiElements.get(id).setBackground(normalColor);
        } else
        {
          uiElements.get(id).setBackground(plausiColor);
        }
      }
    });
  }

  @Override
  public void visibilityChanged(String id, boolean visible)
  {
    SwingUtilities.invokeLater(() -> {
      if (visibilityGroups.containsKey(id))
      {
        for (UIElement element : visibilityGroups.get(id))
        {
          if (element.getComponent() instanceof JButton)
          {
            element.setEnabled(visible);
          } else
          {
            element.setVisible(visible);
          }
        }
      }
    });
  }

  @Override
  public void valueChanged(String id, String value)
  {
    SwingUtilities.invokeLater(() -> {
      if (uiElements.containsKey(id))
      {
        processUIElementEvents = false;
        uiElements.get(id).setString(value);
        processUIElementEvents = true;
      }
    });
  }
}