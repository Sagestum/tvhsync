/*
 * Copyright (c) Henrik Niehaus
 * Copyright (c) 2026 Christian Echterling
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project (Lazy Bones) nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package tvhsync.gui.settings;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tvhsync.TVHsync;
import tvhsync.tvh.Response;
import tvhsync.tvh.Tvheadend;

/**
 * Combobox to select one of the DVR profiles of tvheadend. The profiles are loaded in the background.
 */
public class DvrConfigComboBox extends JComboBox<DvrConfigComboBox.DvrConfig> {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(DvrConfigComboBox.class);

    private String selectedConfig = "";

    public DvrConfigComboBox() {
        addItem(new DvrConfig("", TVHsync.getTranslation("dvr_config_default", "(Default profile)")));
        loadConfigs();
    }

    private void loadConfigs() {
        new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() {
                Response res = Tvheadend.listDvrConfigs();
                return res.isSuccess() ? Tvheadend.getDvrConfigs(res) : null;
            }

            @Override
            protected void done() {
                try {
                    Map<String, String> configs = get();
                    if (configs == null) {
                        return;
                    }
                    for (Entry<String, String> config : configs.entrySet()) {
                        // the default profile has no name
                        if (!config.getValue().isEmpty()) {
                            addItem(new DvrConfig(config.getKey(), config.getValue()));
                        }
                    }
                    setSelectedConfig(selectedConfig);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.warn("Couldn't load DVR profiles", e);
                }
            }
        }.execute();
    }

    public void setSelectedConfig(String uuid) {
        selectedConfig = uuid == null ? "" : uuid;
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).uuid.equals(selectedConfig)) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(0);
    }

    public String getSelectedConfig() {
        DvrConfig selected = (DvrConfig) getSelectedItem();
        if (selected == null || (selected.uuid.isEmpty() && getItemCount() == 1)) {
            // the profiles have not been loaded (yet), keep the current value
            return selectedConfig;
        }
        return selected.uuid;
    }

    public static class DvrConfig {
        private final String uuid;
        private final String name;

        DvrConfig(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
