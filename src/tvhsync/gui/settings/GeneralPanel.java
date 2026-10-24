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

import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import tvhsync.TVHsync;
import tvhsync.tvh.Json;
import tvhsync.tvh.Response;
import tvhsync.tvh.TvhConnection;
import tvhsync.tvh.Tvheadend;
import tvhsync.gui.DebugConsole;

public class GeneralPanel extends JPanel {
	private JTextField url;
	private JTextField user;
	private JPasswordField password;
	private JCheckBox showTimerOptions;
	private JCheckBox logEpgErr;
	private JCheckBox logConnectionErr;
	private JCheckBox supressMatchDialog;
	private JSpinner percentage;
	private JSpinner timeout;

	public GeneralPanel() {
		initGUI();
	}

	private void initGUI() {
		try {
			FlowLayout thisLayout = new FlowLayout();
			thisLayout.setAlignment(FlowLayout.LEFT);
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(517, 343));

			var mainPanel = new JPanel();
			GridBagLayout mainPanelLayout = new GridBagLayout();
			mainPanelLayout.columnWidths = new int[] { 7, 7 };
			mainPanelLayout.rowHeights = new int[] { 7, 7, 7, 7 };
			mainPanelLayout.columnWeights = new double[] { 0.1, 0.1 };
			mainPanelLayout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.1 };
			this.add(mainPanel);
			mainPanel.setLayout(mainPanelLayout);

			var lHost = new JLabel();
			lHost.setText(TVHsync.getTranslation("tvh_url", "tvheadend URL"));
			mainPanel.add(lHost, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			var expertsPanel = new JPanel();
			GridBagLayout expertsPanelLayout = new GridBagLayout();
			expertsPanel.setLayout(expertsPanelLayout);
			expertsPanel.setBorder(BorderFactory.createTitledBorder(TVHsync.getTranslation("experts", "Experts")));

			var lFuzzyness = new JLabel();
			lFuzzyness.setText(TVHsync.getTranslation("percentageOfEquality", "Fuzzylevel program titles"));

			logConnectionErr = new JCheckBox();
			expertsPanel.add(logConnectionErr, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			logConnectionErr.setSelected(
					Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("logConnectionErrors")));

			var lLogConnectionErr = new JLabel(
					TVHsync.getTranslation("logConnectionErrors", "Show error dialogs on connection problems"));
			lLogConnectionErr.setText(
					TVHsync.getTranslation("logConnectionErrors", "Show error dialogs on connection problems"));

			int percentageThreshold = Integer.parseInt(TVHsync.getProperties().getProperty("percentageThreshold"));
			SpinnerModel percentageModel = new SpinnerNumberModel(percentageThreshold, 0, 100, 1);
			percentage = new JSpinner();
			percentage.setModel(percentageModel);
			percentage.setToolTipText(TVHsync.getTranslation("percentageOfEquality.tooltip",
					"Percentage of equality of program titles"));

			supressMatchDialog = new JCheckBox();
			expertsPanel.add(supressMatchDialog, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			expertsPanel.add(percentage, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			expertsPanel.add(lFuzzyness, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			supressMatchDialog.setToolTipText(TVHsync.getTranslation("supressMatchDialog.tooltip",
					"Do not show EPG selection dialog for non matching tvheadend timers"));
			supressMatchDialog.setSelected(
					Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("supressMatchDialog")));

			var lSupressMatchDialog = new JLabel();
			expertsPanel.add(lSupressMatchDialog, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			expertsPanel.add(lLogConnectionErr, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			var lLogEpgErr = new JLabel();
			expertsPanel.add(lLogEpgErr, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			lLogEpgErr.setText(TVHsync.getTranslation("logEPGErrors", "Show error dialogs, if EPG data are missing"));

			logEpgErr = new JCheckBox();
			logEpgErr
					.setSelected(Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("logEPGErrors")));
			expertsPanel.add(logEpgErr, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			var lShowTimerOptions = new JLabel();
			expertsPanel.add(lShowTimerOptions, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			lShowTimerOptions.setText(
					TVHsync.getTranslation("showTimerOptionsDialog", "Show timer options dialog on timer creation"));

			showTimerOptions = new JCheckBox();
			showTimerOptions.setSelected(
					Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("showTimerOptionsDialog")));
			expertsPanel.add(showTimerOptions, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			var bShowLog = new JButton();
			expertsPanel.add(bShowLog, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			bShowLog.setText(TVHsync.getTranslation("show_log", "Show log"));
			bShowLog.addActionListener(evt -> {
				DebugConsole dc = new DebugConsole();
				dc.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
				dc.setVisible(true);
			});

			lSupressMatchDialog.setText(TVHsync.getTranslation("supressMatchDialog", "Supress match dialog"));

			expertsPanelLayout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1 };
			expertsPanelLayout.rowHeights = new int[] { 7, 7, 7, 7, 7, 7 };
			expertsPanelLayout.columnWeights = new double[] { 0.1, 0.1 };
			expertsPanelLayout.columnWidths = new int[] { 7, 7 };

			url = new JTextField(25);
			url.setText(TVHsync.getProperties().getProperty("url"));
			url.setToolTipText("http://host:9981");
			mainPanel.add(url, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

			var lUser = new JLabel(TVHsync.getTranslation("user", "User"));
			mainPanel.add(lUser, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			user = new JTextField();
			user.setText(TVHsync.getProperties().getProperty("user"));
			mainPanel.add(user, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

			var lPassword = new JLabel(TVHsync.getTranslation("password", "Password"));
			mainPanel.add(lPassword, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			password = new JPasswordField();
			password.setText(TVHsync.getProperties().getProperty("password"));
			mainPanel.add(password, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

			int value = Integer.parseInt(TVHsync.getProperties().getProperty("timeout"));
			SpinnerNumberModel timeoutModel = new SpinnerNumberModel(value, 1, 60000, 100);
			timeout = new JSpinner();
			timeout.setModel(timeoutModel);
			timeout.setEditor(new JSpinner.NumberEditor(timeout, "#"));
			mainPanel.add(timeout, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

			var lTimeout = new JLabel();
			lTimeout.setText(TVHsync.getTranslation("timeout", "Timeout (ms)"));
			mainPanel.add(lTimeout, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			var bTest = new JButton(TVHsync.getTranslation("test_connection", "Test connection"));
			bTest.addActionListener(evt -> testConnection());
			mainPanel.add(bTest, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			mainPanel.add(expertsPanel, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			expertsPanel.setSize(10, 10);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public GeneralPanel getPanel() {
		return this;
	}

	/**
	 * Applies the connection settings currently entered, so that they can be used before the settings are saved
	 */
	public void applyConnectionSettings() {
		TvhConnection.url = url.getText().trim();
		TvhConnection.user = user.getText().trim();
		TvhConnection.password = new String(password.getPassword());
		TvhConnection.timeout = (Integer) timeout.getValue();
	}

	private void testConnection() {
		applyConnectionSettings();
		Response res = Tvheadend.getServerInfo();
		if (res.isSuccess()) {
			String version = Json.getString(Json.asMap(res.getBody()), "sw_version");
			JOptionPane.showMessageDialog(this, TVHsync.getTranslation("connection_ok", "Connected to tvheadend {0}", version));
		} else {
			JOptionPane.showMessageDialog(this, res.getMessage(), "", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void saveSettings() {
		applyConnectionSettings();
		TVHsync.getProperties().setProperty("url", TvhConnection.url);
		TVHsync.getProperties().setProperty("user", TvhConnection.user);
		TVHsync.getProperties().setProperty("password", TvhConnection.password);
		TVHsync.getProperties().setProperty("timeout", Integer.toString(TvhConnection.timeout));
		TVHsync.getProperties().setProperty("percentageThreshold", percentage.getValue().toString());
		TVHsync.getProperties().setProperty("supressMatchDialog", Boolean.toString(supressMatchDialog.isSelected()));
		TVHsync.getProperties().setProperty("logConnectionErrors", Boolean.toString(logConnectionErr.isSelected()));
		TVHsync.getProperties().setProperty("logEPGErrors", Boolean.toString(logEpgErr.isSelected()));
		TVHsync.getProperties().setProperty("showTimerOptionsDialog",
				Boolean.toString(showTimerOptions.isSelected()));
	}

}