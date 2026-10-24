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

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTHWEST;
import static java.awt.GridBagConstraints.WEST;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import tvhsync.TVHsync;


public class PlayerPanel {

    private final String lSurviveOnExit = TVHsync.getTranslation("survive_on_exit", "Keep player running when TV-Browser closes");

    private JPanel panel;
    private JTextField urlRecordings;
    private JTextField url;
    private JCheckBox surviveOnExit;
    private JTextField params;
    private JTextField player;

    public PlayerPanel() {
        initComponents();
    }

    private void initComponents() {
        panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        GridBagLayout panelLayout = new GridBagLayout();
        panel.setLayout(panelLayout);
        panel.setPreferredSize(new java.awt.Dimension(346, 183));

        Insets defaultInsets = new Insets(5, 5, 5, 5);

        var container = new JPanel();
        panel.add(container, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, NORTHWEST, HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        GridBagLayout containerLayout = new GridBagLayout();
        container.setLayout(containerLayout);

        var lPlayer = new JLabel();
        container.add(lPlayer, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, WEST, NONE, defaultInsets, 0, 0));
        lPlayer.setText(TVHsync.getTranslation("player", "Player"));

        player = new JTextField(TVHsync.getProperties().getProperty("player"));
        container.add(player, new GridBagConstraints(1, 0, 1, 1, 0.9, 0.1, WEST, HORIZONTAL, defaultInsets, 0, 0));

        var lParams = new JLabel();
        container.add(lParams, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, WEST, NONE, defaultInsets, 0, 0));
        lParams.setText(TVHsync.getTranslation("params", "Parameters"));

        params = new JTextField(TVHsync.getProperties().getProperty("player_params"));
        container.add(params, new GridBagConstraints(1, 1, 1, 1, 0.9, 0.1, WEST, HORIZONTAL, defaultInsets, 0, 0));

        var lPlaceholders = new JLabel(TVHsync.getTranslation("url_placeholders",
                "<html>&lt;url&gt; = tvheadend URL, &lt;channel&gt; = channel UUID, &lt;recording&gt; = recording UUID</html>"));
        container.add(lPlaceholders, new GridBagConstraints(1, 4, 1, 1, 0.9, 0.1, WEST, HORIZONTAL, defaultInsets, 0, 0));

        surviveOnExit = new JCheckBox(lSurviveOnExit);
        container.add(surviveOnExit, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, WEST, NONE, defaultInsets, 0, 0));

        var lUrlRecordings = new JLabel();
        container.add(lUrlRecordings, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, WEST, NONE, defaultInsets, 0, 0));
        lUrlRecordings.setText(TVHsync.getTranslation("url", "URL") + " " + TVHsync.getTranslation("recordings", "Recordings"));

        var lURL = new JLabel();
        container.add(lURL, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, WEST, NONE, defaultInsets, 0, 0));
        lURL.setText(TVHsync.getTranslation("url", "URL"));

        url = new JTextField(TVHsync.getProperties().getProperty("streamurl"));
        container.add(url, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, WEST, HORIZONTAL, defaultInsets, 0, 0));


        urlRecordings = new JTextField(TVHsync.getProperties().getProperty("recording.url"));
        container.add(urlRecordings, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, WEST, HORIZONTAL, defaultInsets, 0, 0));


        surviveOnExit.setSelected(Boolean.parseBoolean(TVHsync.getProperties().getProperty("surviveOnExit")));
    }

    public void saveSettings() {
        TVHsync.getProperties().setProperty("player", player.getText());
        TVHsync.getProperties().setProperty("player_params", params.getText());
        TVHsync.getProperties().setProperty("streamurl", url.getText());
        TVHsync.getProperties().setProperty("recording.url", urlRecordings.getText());
        TVHsync.getProperties().setProperty("surviveOnExit", Boolean.toString(surviveOnExit.isSelected()));
    }

    public JPanel getPanel() {
        return panel;
    }

}