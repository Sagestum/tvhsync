/*
 * Copyright (c) Henrik Niehaus & Lazy Bones development team
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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import tvhsync.TVHsync;
import tvhsync.TimerManager;
import tvhsync.gui.settings.channelpanel.ChannelPanel;
import util.ui.ImageUtilities;
import util.i18n.Localizer;

/**
 * The root container for the settings tabs
 *
 * @author <a href="hampelratte@users.sf.net">hampelratte@users.sf.net </a>
 */
public class SettingsPanel implements devplugin.SettingsTab {

    private JTabbedPane tabbedPane;

    private ChannelPanel channelPanel;

    private GeneralPanel generalPanel;

    private PlayerPanel playerPanel;

    private TimerPanel timerPanel;

    private TimerManager timerManager;

    public SettingsPanel(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    @Override
    public JPanel createSettingsPanel() {
        if (tabbedPane == null) {
            tabbedPane = new JTabbedPane();
            tabbedPane.setPreferredSize(new Dimension(380, 380));

            generalPanel = new GeneralPanel();
            tabbedPane.addTab(TVHsync.getTranslation("general", "General"), generalPanel.getPanel());

            channelPanel = new ChannelPanel();
            // use the connection settings, which have been entered, but not saved yet
            channelPanel.setBeforeRefresh(generalPanel::applyConnectionSettings);
            tabbedPane.addTab(Localizer.getLocalization(Localizer.I18N_CHANNELS), channelPanel.getPanel());

            playerPanel = new PlayerPanel();
            tabbedPane.addTab(TVHsync.getTranslation("player", "Player"), playerPanel.getPanel());

            timerPanel = new TimerPanel(timerManager);
            tabbedPane.addTab(TVHsync.getTranslation("timers", "Timers"), timerPanel.getPanel());
        }

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(tabbedPane, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void saveSettings() {
        channelPanel.saveSettings();
        generalPanel.saveSettings();
        playerPanel.saveSettings();
        timerPanel.saveSettings();
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(ImageUtilities.createImageFromJar("tvhsync/tvhsync16.png", SettingsPanel.class));
    }

    @Override
    public String getTitle() {
        return "TVHsync";
    }
}