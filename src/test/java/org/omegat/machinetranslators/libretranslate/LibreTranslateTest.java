package org.omegat.machinetranslators.libretranslate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.omegat.core.TestCore;
import org.omegat.util.Language;
import org.omegat.util.Preferences;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class LibreTranslateTest extends TestCore {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Test
    public void testGetJsonResults() throws Exception {
        Preferences.setPreference(LibreTranslate.ALLOW_LIBRE_TRANSLATE, true);
        LibreTranslate translator = new LibreTranslate("https://localhost/");
        String json = "{\n    \"translatedText\": \"¡Hola!\"\n}";
        String result = translator.getJsonResults(json);
        Assert.assertEquals("¡Hola!", result);
    }

    @Test
    public void testGetJsonResultsWithWrongJson() {
        Preferences.setPreference(LibreTranslate.ALLOW_LIBRE_TRANSLATE, true);
        LibreTranslate translator = new LibreTranslate("https://localhost/");
        String json = "{ \"response\": \"failed\" }";
        assertThrows(Exception.class, () -> translator.getJsonResults(json));
    }

    @Test
    public void testResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Preferences.setPreference(LibreTranslate.ALLOW_LIBRE_TRANSLATE, true);
        String sourceText = "Hello, how are you today?";
        String encoded = URLEncoder.encode(sourceText, StandardCharsets.UTF_8);
        JsonNode json = mapper.readTree("{\n  \"translatedText\": \n      \"Hola, ¿cómo estás hoy?\"\n}");

        WireMock.stubFor(WireMock.post(WireMock.anyUrl())
                .withHeader("Content-Type", WireMock.equalTo("application/x-www-form-urlencoded"))
                .withRequestBody(WireMock.and(
                        WireMock.containing("q=" + encoded),
                        WireMock.containing("source=en"),
                        WireMock.containing("target=es")
                ))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withJsonBody(json)));

        int port = wireMockRule.port();
        String url = String.format("http://localhost:%d/translate", port);
        LibreTranslate translator = new LibreTranslate(url);
        String result = translator.translate(new Language("en"), new Language("es"), sourceText);
        assertEquals("Hola, ¿cómo estás hoy?", result);
    }
}
