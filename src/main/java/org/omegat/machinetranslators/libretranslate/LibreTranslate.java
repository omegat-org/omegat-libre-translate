/*
 *  OmegaT - Computer Assisted Translation (CAT) tool
 *           with fuzzy matching, translation memory, keyword search,
 *           glossaries, and translation leveraging into updated projects.
 *
 *  Copyright (C) 2010 Alex Buloichik, Didier Briel
 *                2011 Briac Pilpre, Alex Buloichik
 *                2013 Didier Briel
 *                2016 Aaron Madlon-Kay
 *                2021,2023 Hiroshi Miura
 *                Home page: https://www.omegat.org/
 *                Support center: https://omegat.org/support
 *
 *  This file is part of OmegaT.
 *
 *  OmegaT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OmegaT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.omegat.machinetranslators.libretranslate;

import org.omegat.core.Core;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.HttpConnectionUtils;
import org.omegat.util.Language;
import org.omegat.util.Preferences;

import java.awt.Dimension;
import java.awt.Window;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support of Libre Translate.
 *
 * @author Hiroshi Miura
 *
 */
public class LibreTranslate extends BaseCachedTranslate {
    private static final String DEFAULT_URL = "https://libretranslate.com/translate";
    private static final ResourceBundle BUNDLE =
            ResourceBundle.getBundle("org.omegat.machinetranslators.libretranslate.Bundle");
    private static final Logger LOGGER = LoggerFactory.getLogger(LibreTranslate.class);

    public static final String ALLOW_LIBRE_TRANSLATE = "allow.libre.translate";
    private static final String LIBRE_TRANSLATE_SERVER_URL = "libre.translate.server.url";
    protected String serverUrl;

    /**
     * Register plugins into OmegaT.
     */
    @SuppressWarnings("unused")
    public static void loadPlugins() {
        Core.registerMachineTranslationClass(LibreTranslate.class);
    }

    @SuppressWarnings("unused")
    public static void unloadPlugins() {}

    @SuppressWarnings("unused")
    public LibreTranslate() {
        serverUrl = Preferences.getPreferenceDefault(LIBRE_TRANSLATE_SERVER_URL, DEFAULT_URL);
    }

    /**
     * Constructor for tests.
     * @param baseUrl custom base url
     */
    public LibreTranslate(String baseUrl) {
        serverUrl = baseUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPreferenceName() {
        return ALLOW_LIBRE_TRANSLATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return BUNDLE.getString("MT_ENGINE_LIBRE_TRANSLATE");
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("q", text);
        params.put("source", sLang.getLanguageCode().toLowerCase());
        params.put("target", tLang.getLanguageCode().toLowerCase());
        Map<String, String> headers = new TreeMap<>();
        String v = HttpConnectionUtils.post(serverUrl, params, headers);
        String tr = getJsonResults(v);
        if (tr == null) {
            return null;
        }
        return cleanSpacesAroundTags(tr, text);
    }

    /**
     * Parse API response and return translated text.
     * @param json
     *            API response json string.
     * @return translation, or null when API returns an empty result,
     *            or error message when parse failed.
     */
    protected String getJsonResults(String json) throws MachineTranslateError {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode translations = rootNode.get("translatedText");
            if (translations != null) {
                return translations.asText();
            }
            LOGGER.error(BUNDLE.getString("MT_JSON_ERROR"));
        } catch (Exception e) {
            LOGGER.error(BUNDLE.getString("MT_JSON_ERROR"), e);
        }
        throw new MachineTranslateError(BUNDLE.getString("MT_JSON_ERROR"));
    }

    /**
     * Engine is configurable.
     *
     * @return true
     */
    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void showConfigurationUI(Window parent) {

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                Preferences.setPreference(LIBRE_TRANSLATE_SERVER_URL, panel.valueField1.getText());
            }
        };

        dialog.panel.valueLabel1.setText(BUNDLE.getString("MT_ENGINE_LIBRE_TRANSLATE_URL_LABEL"));
        dialog.panel.valueField1.setText(Preferences.getPreference(LIBRE_TRANSLATE_SERVER_URL));
        dialog.panel.valueField1.setPreferredSize(new Dimension(150, 20));
        dialog.panel.valueLabel2.setVisible(false);
        dialog.panel.valueField2.setVisible(false);
        dialog.show();
    }
}
