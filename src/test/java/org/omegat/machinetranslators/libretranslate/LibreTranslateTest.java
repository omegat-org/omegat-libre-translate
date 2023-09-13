package org.omegat.machinetranslators.libretranslate;

import org.omegat.core.TestCore;
import org.omegat.util.Preferences;

import org.junit.Assert;
import org.junit.Test;

public class LibreTranslateTest extends TestCore {

    @Test
    public void testGetJsonResults() throws Exception {
        Preferences.setPreference(LibreTranslate.ALLOW_LIBRE_TRANSLATE, true);
        LibreTranslate translator = new LibreTranslate("https://localhost/");
        String json = "{\n    \"translatedText\": \"¡Hola!\"\n}";
        String result = translator.getJsonResults(json);
        Assert.assertEquals("¡Hola!", result);
    }
}
