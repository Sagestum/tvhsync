/*
 * Copyright (c) Henrik Niehaus
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
package tvhsync.gui.components.timeroptions;

import devplugin.Plugin;
import devplugin.Program;
import devplugin.ProgramFieldType;
import tvhsync.*;
import tvhsync.ChannelManager.ChannelNotFoundException;
import tvhsync.gui.components.daychooser.BrowseTextField;
import tvhsync.gui.components.daychooser.DayChooser;
import tvhsync.gui.components.timeroptions.TimerOptionsDialog.Mode;
import tvhsync.gui.settings.DescriptionComboBoxModel;
import tvhsync.gui.settings.DescriptionSelectorItem;
import tvhsync.gui.settings.DvrConfigComboBox;
import tvhsync.gui.settings.PriorityComboBox;
import tvhsync.gui.settings.SeriesTitleSelectorItem;
import tvhsync.programmanager.ProgramDatabase;
import tvhsync.programmanager.ProgramManager;
import tvhsync.model.Channel;
import tvhsync.model.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tvbrowser.core.ChannelList;
import util.i18n.Localizer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import static tvhsync.gui.settings.DescriptionSelectorItem.*;

public class TimerOptionsEditor extends JPanel implements ItemListener, WindowListener, ChangeListener {
    private static final transient Logger logger = LoggerFactory.getLogger(TimerOptionsEditor.class);

    private final JLabel lChannels = new JLabel(Localizer.getLocalization(Localizer.I18N_CHANNEL));

    private final JComboBox<Object> channels = new JComboBox<>();

    private final JLabel lDay = new JLabel(TVHsync.getTranslation("day", "Day"));

    private DayChooser dayChooser;

    private BrowseTextField day;

    private final JLabel lStarttime = new JLabel(TVHsync.getTranslation("start", "Start"));

    private SpinnerCalendarModel spinnerStarttimeModel;
    private JSpinner spinnerStarttime;

    private final JLabel lEndtime = new JLabel(TVHsync.getTranslation("stop", "Stop"));

    private SpinnerCalendarModel spinnerEndtimeModel;
    private JSpinner spinnerEndtime;

    private final JLabel lPriority = new JLabel(TVHsync.getTranslation("priority", "Priority"));

    private final PriorityComboBox priority = new PriorityComboBox();

    private final JLabel lLifetime = new JLabel(TVHsync.getTranslation("lifetime", "Lifetime"));

    private final JLabel lDvrConfig = new JLabel(TVHsync.getTranslation("dvr_config", "DVR profile"));

    private final DvrConfigComboBox dvrConfig = new DvrConfigComboBox();

    private final JSpinner lifetime = new JSpinner();

    private final JLabel lTitle = new JLabel(TVHsync.getTranslation("title", "Title"));

    private final JTextField title = new JTextField();

    private final JLabel lDescription = new JLabel(TVHsync.getTranslation("description", "Description"));

    private DescriptionComboBoxModel comboDescModel;
    private final JComboBox<DescriptionSelectorItem> comboDesc = new JComboBox<>();

    private final JTextArea description = new JTextArea();

    private final JCheckBox cbActive = new JCheckBox(TVHsync.getTranslation("active", "Active"));

    private final JCheckBox cbSeries = new JCheckBox(TVHsync.getTranslation("series", "Series"));


    private final transient TimerManager timerManager;
    private final transient RecordingManager recordingManager;

    /**
     * The actual timer
     */
    private TvhTimer timer;

    /**
     * A clone of the timer containing the old settings
     */
    private TvhTimer oldTimer;

    private transient Program prog;

    private final Mode mode;

    private String originalTitel = "";

    public TimerOptionsEditor(TimerManager timerManager, RecordingManager recordingManager, TvhTimer timer,
                              Mode mode) {
        this.timerManager = timerManager;
        this.recordingManager = recordingManager;
        this.mode = mode;

        logger.debug("Creating timer options panel");
        initGUI();

        setTimer(timer);

        logger.debug("Timer options panel ready");
    }

    public TimerOptionsEditor(TimerManager timerManager, RecordingManager recordingManager, Mode mode) {
        this.timerManager = timerManager;
        this.recordingManager = recordingManager;
        this.mode = mode;
        initGUI();
    }

    private void initGUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;

        int row = 0;
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        add(createCheckboxGrid(), gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(lTitle, gbc);


        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lChannels, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lDay, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lStarttime, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lEndtime, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lPriority, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lLifetime, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lDvrConfig, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        add(lDescription, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        description.setRows(10);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        add(new JScrollPane(description), gbc);

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 1;
        row = 1;
        gbc.gridy = row++;
        add(title, gbc);


        gbc.gridx = 1;
        gbc.gridy = row++;
        add(channels, gbc);
        devplugin.Channel[] c = ChannelList.getSubscribedChannels();
        for (devplugin.Channel channel : c) {
            channels.addItem(channel);
        }

        gbc.gridx = 1;
        gbc.gridy = row++;
        dayChooser = new DayChooser();
        day = new BrowseTextField(dayChooser);
        day.setEditable(false);
        add(day, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        spinnerStarttime = new JSpinner();
        spinnerStarttime.addChangeListener(this);
        add(spinnerStarttime, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        spinnerEndtime = new JSpinner();
        spinnerEndtime.addChangeListener(this);
        add(spinnerEndtime, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        add(priority, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        add(lifetime, gbc);
        lifetime.setToolTipText(TVHsync.getTranslation("lifetime.tooltip", "Days until the recording gets deleted. 0 = setting of the DVR profile"));

        gbc.gridx = 1;
        gbc.gridy = row++;
        add(dvrConfig, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        add(comboDesc, gbc);

        comboDescModel = new DescriptionComboBoxModel(false, mode == Mode.UPDATE);
        comboDesc.setModel(comboDescModel);
        comboDesc.addItemListener(this);
    }

    private Component createCheckboxGrid() {
        JPanel checkboxGrid = new JPanel(new GridLayout(1, 2));

        checkboxGrid.add(cbActive);

        cbSeries.addActionListener(this::seriesCheckboxActionHandler);
        checkboxGrid.add(cbSeries);

        return checkboxGrid;
    }

    public void setTimer(TvhTimer timer) {
        logger.debug("Updating gui with timer values");
        if (timer != null) {
            this.timer = timer;
            this.prog = null;
            this.oldTimer = (TvhTimer) timer.clone();

            Program program = getProgramForTimer(timer);
            selectChannel(program);
            setDescription(program);

            // set timer is active switch
            cbActive.setSelected(timer.isActive());

            // set timer title
            title.setText(timer.getTitle());


            setStartAndEndTime();
            priority.setPriority(timer.getPriority());
            lifetime.setModel(new SpinnerNumberModel(timer.getLifetime(), 0, Integer.MAX_VALUE, 1));
            lifetime.setEditor(new JSpinner.NumberEditor(lifetime, "#"));
            dvrConfig.setSelectedConfig(timer.getDvrConfig());
            dayChooser.setTimer(timer, this);
            day.setText(DayChooser.getDayString(timer));
        }
    }

    private void setStartAndEndTime() {
        // initialize the start and end time spinners
        spinnerStarttimeModel = new SpinnerCalendarModel(timer.getStartTime());
        spinnerStarttime.setModel(spinnerStarttimeModel);
        SpinnerCalendarEditor editor = new SpinnerCalendarEditor(spinnerStarttime, spinnerStarttimeModel);
        spinnerStarttime.setEditor(editor);
        spinnerEndtimeModel = new SpinnerCalendarModel(timer.getEndTime());
        spinnerEndtime.setModel(spinnerEndtimeModel);
        editor = new SpinnerCalendarEditor(spinnerEndtime, spinnerEndtimeModel);
        spinnerEndtime.setEditor(editor);
    }

    private void setDescription(Program program) {
        if (mode == Mode.UPDATE) {
            description.setText(oldTimer.getDescription());
            comboDescModel.setSelected(DescriptionSelectorItem.TIMER);
        } else {
            String descTvh = timer.getDescription() == null ? "" : timer.getDescription();
            String selectedDescriptionId = TVHsync.getProperties().getProperty("descSourceTvb");
            String descriptionText = TvhTimer.createDescription(selectedDescriptionId, descTvh, program);

            description.setText(descriptionText);
            comboDescModel.setSelected(selectedDescriptionId);
        }
    }

    private void selectChannel(Program program) {
        if (program != null) {
            channels.setSelectedItem(program.getChannel());
        } else {
            Channel chan = ChannelManager.getInstance().getChannelById(timer.getChannelId());
            if (chan != null) {
                channels.addItem(chan);
                channels.setSelectedItem(chan);
            }
        }
    }

    private Program getProgramForTimer(TvhTimer timer) {
        // we have to remove the buffers again, to get the right start date
        // example: start time is 00.00 h with time buffers we have 23.45
        // Calendar then decreases the start date, so that we don't have the right date,
        // but
        // the date of the day before.
        TvhTimer tmp = timer.getTimerWithoutBuffers();
        Program program = null;
        try {
            program = ProgramDatabase.getProgram(tmp);
            this.prog = program;
        } catch (ChannelNotFoundException e) {
            // fail silently
        }
        return program;
    }

    private void seriesCheckboxActionHandler(ActionEvent e) {
        if (cbSeries.isSelected()) {
            originalTitel = title.getText();

            String format = TVHsync.getProperties().getProperty("timer.series.title");
            if (SeriesTitleSelectorItem.TVB.equals(format) && prog != null) {
                String episodeNumber = prog.getIntFieldAsString(ProgramFieldType.EPISODE_NUMBER_TYPE);
                String episode = prog.getTextField(ProgramFieldType.EPISODE_TYPE);
                String series = prog.getTitle();
                logger.debug("Series {}- {} {}", series, episodeNumber, episode);

                if (isNotBlank(episodeNumber) || isNotBlank(episode)) {
                    title.setText(series + " - " + createEpisodeName(episode, episodeNumber));
                }
            }
        } else {
            if (originalTitel.trim().isEmpty() && prog != null) {
                title.setText(prog.getTitle());
            } else {
                title.setText(originalTitel);
            }
        }
    }

    private String createEpisodeName(String episode, String episodeNumber) {
        StringBuilder episodeSb = new StringBuilder();
        if (isNotBlank(episodeNumber)) {
            episodeSb.append(episodeNumber);
            if (isNotBlank(episode)) {
                episodeSb.append(" - ");
            }
        }
        if (isNotBlank(episode)) {
            episodeSb.append(episode);
        }
        return episodeSb.toString();
    }

    private boolean isNotBlank(String s) {
        return s != null && s.trim().length() != 0;
    }

    public TvhTimer getOldTimer() {
        return oldTimer;
    }

    public TvhTimer getTimer() {
        timer.setTitle(title.getText());
        Channel tvhChannel = null;
        Object selected = channels.getSelectedItem();
        if (selected instanceof devplugin.Channel) {
            devplugin.Channel c = (devplugin.Channel) selected;
            tvhChannel = ChannelManager.getChannelMapping().get(c.getId());
        } else if (selected instanceof Channel) {
            tvhChannel = (Channel) selected;
        }
        Objects.requireNonNull(tvhChannel, "Couldn't determine tvheadend channel for " + selected);
        timer.setChannelId(tvhChannel.getId());
        timer.setPriority(priority.getPriority());
        timer.setLifetime((Integer) lifetime.getValue());
        timer.setDvrConfig(dvrConfig.getSelectedConfig());
        timer.setDescription(description.getText());
        timer.changeStateTo(Timer.ACTIVE, cbActive.isSelected());

        Calendar startTime = (Calendar) spinnerStarttimeModel.getValue();
        timer.getStartTime().setTimeInMillis(startTime.getTimeInMillis());
        timer.getStartTime().set(Calendar.SECOND, 0);
        timer.getStartTime().set(Calendar.MILLISECOND, 0);

        Calendar endTime = (Calendar) spinnerEndtimeModel.getValue();
        timer.getEndTime().setTimeInMillis(endTime.getTimeInMillis());
        timer.getEndTime().set(Calendar.SECOND, 0);
        timer.getEndTime().set(Calendar.MILLISECOND, 0);
        return timer;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            DescriptionSelectorItem selectedDescription = (DescriptionSelectorItem) e.getItem();
            changeDescription(selectedDescription);
        }
    }

    private void changeDescription(DescriptionSelectorItem selectedDescription) {
        if (selectedDescription.getId().equals(TIMER)) {
            description.setText(oldTimer.getDescription());
        } else if (selectedDescription.getId().equals(TVH)) {
            description.setText(getDescriptionFromTvh());
            getParent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } else if (selectedDescription.getId().equals(TVB_DESC)) {
            description.setText(getDescriptionFromTvb());
        } else if (selectedDescription.getId().startsWith(TVB_PREFIX)) {
            Program program;
            if (this.prog != null) {
                program = this.prog;
            } else {
                program = Plugin.getPluginManager().getProgram(timer.getTvBrowserProgIDs().get(0));
            }
            String desc = TvhTimer.createDescription(selectedDescription.getId(), "", program);
            description.setText(desc);
        }
    }

    private String getDescriptionFromTvb() {
        Program program;
        if (this.prog != null) {
            program = this.prog;
        } else {
            program = Plugin.getPluginManager().getProgram(timer.getTvBrowserProgIDs().get(0));
        }

        if (program != null && program.getDescription() != null) {
            return program.getDescription();
        } else {
            return "";
        }
    }

    private String getDescriptionFromTvh() {
        Program program = Plugin.getPluginManager().getProgram(timer.getTvBrowserProgIDs().get(0));
        Calendar tmpCal = (Calendar) timer.getStartTime().clone();
        tmpCal.set(Calendar.HOUR_OF_DAY, program.getHours());
        tmpCal.set(Calendar.MINUTE, program.getMinutes());
        tmpCal.add(Calendar.MINUTE, program.getLength() / 2); // take the middle of the playtime to be sure, that the
        // right program is chosen
        getParent().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        Timer tmp = ProgramManager.getInstance().getTimerForTime(tmpCal, program.getChannel());
        return tmp == null ? "" : tmp.getDescription();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        // noop, not interested in this event
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // noop, not interested in this event
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // noop, not interested in this event
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // noop, not interested in this event
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // noop, not interested in this event
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // noop, not interested in this event
    }

    @Override
    public void windowOpened(WindowEvent e) {
        title.requestFocus();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == spinnerStarttime) {
            Calendar startTime = (Calendar) spinnerStarttimeModel.getValue();
            timer.setStartTime(startTime);
            dayChooser.setTimer(timer, this);
            day.setText(DayChooser.getDayString(timer));
        }
    }

    public void startTimeChanged() {
        spinnerStarttimeModel.setValue(timer.getStartTime());
        day.setText(DayChooser.getDayString(timer));
    }
}
