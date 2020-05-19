package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class AndFunctionTest
{

  @Test
  public void andFunction() throws Exception
  {
    Function f = new AndFunction(new ConfigThingy("AND", "\"true\" \"true\""), new FunctionLibrary(),
        new DialogLibrary(), new HashMap<>());
    assertEquals(0, f.parameters().length);
    assertEquals("true", f.getString(null));
    assertTrue(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new AndFunction(List.of(new StringLiteralFunction("true"), new StringLiteralFunction("false")));
    assertEquals("false", f.getString(null));
    assertFalse(f.getBoolean(null));

    f = new AndFunction(List.of(new StringLiteralFunction(FunctionLibrary.ERROR)));
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
  }

}
