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
package tvhsync;

import com.thoughtworks.xstream.XStream;
import devplugin.*;
import tvhsync.gui.MainDialog;
import tvhsync.gui.RecordingsCenterPanel;
import tvhsync.gui.TimelineCenterPanel;
import tvhsync.gui.TimersCenterPanel;
import tvhsync.gui.settings.DescriptionSelectorItem;
import tvhsync.gui.settings.SeriesTitleSelectorItem;
import tvhsync.gui.settings.SettingsPanel;
import tvhsync.logging.DebugConsoleHandler;
import tvhsync.logging.PopupHandler;
import tvhsync.logging.SimpleFormatter;
import tvhsync.programmanager.ProgramDatabase;
import tvhsync.programmanager.ProgramManager;
import tvhsync.actions.CreateTimerAction;
import tvhsync.model.Channel;
import tvhsync.tvh.TvhConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tvbrowser.core.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * A plugin to manage the timers and recordings of a tvheadend server.
 *
 * Based on the VDR plugin Lazy Bones by Henrik Niehaus.
 *
 * @author <a href="hampelratte@users.sf.net">hampelratte@users.sf.net</a>
 *
 */
public class TVHsync extends Plugin implements TimersChangedListener {

    private static final Logger logger = LoggerFactory.getLogger(TVHsync.class);

    /** Translator */
    private static final util.i18n.Localizer mLocalizer = util.i18n.Localizer.getLocalizerFor(TVHsync.class);

    private MainDialog mainDialog;

    private static Properties props;

    private final ContextMenuFactory cmf = new ContextMenuFactory();

    private static TVHsync instance;

    public static final String TIMER_MENU_KEY = "TIMER_MENU_KEY";

    private PluginCenterPanelWrapper wrapper;

    private final TimerManager timerManager;
    private final RecordingManager recordingManager;

    public TVHsync() {
        timerManager = new TimerManager();
        recordingManager = new RecordingManager();
        timerManager.setRecordingManager(recordingManager);
        recordingManager.setTimerManager(timerManager);
    }

    public static TVHsync getInstance() {
        return instance;
    }

    @Override
    public ActionMenu getContextMenuActions(final Program program) {
        return cmf.createActionMenu(program);
    }

    @Override
    public ActionMenu getContextMenuActions(final devplugin.Channel channel) {
        return cmf.createChannelActionMenu(channel);
    }

    @Override
    public PluginCenterPanelWrapper getPluginCenterPanelWrapper() {
        return wrapper;
    }

    @Override
    public ActionMenu getButtonAction() {
        var buttonAction = new ButtonAction();
        buttonAction.setActionListener(e -> getMainDialog().setVisible(true));
        buttonAction.setBigIcon(createImageIcon("tvhsync/tvhsync24.png"));
        buttonAction.setSmallIcon(createImageIcon("tvhsync/tvhsync16.png"));
        buttonAction.setShortDescription(TVHsync.getTranslation("tvhsync", "TVHsync"));
        buttonAction.setText(TVHsync.getTranslation("tvhsync", "TVHsync"));
        return new ActionMenu(buttonAction);
    }

    @Override
    public String getPluginCategory() {
        return Plugin.CATEGORY_REMOTE_CONTROL_SOFTWARE;
    }

    /**
     * Called by TimerSelectionDialog, if a tvheadend program has been selected
     *
     * @param selectedProgram
     */
    public void timerSelectionCallBack(Program selectedProgram) {
        int bufferBefore = Integer.parseInt(props.getProperty("timer.before"));
        int bufferAfter = Integer.parseInt(props.getProperty("timer.after"));

        TvhTimer t = ((TimerProgram) selectedProgram).getTimer();
        // start the recording x min before the beginning of the program
        t.getStartTime().add(Calendar.MINUTE, -bufferBefore);
        t.setPreBuffer(bufferBefore);
        // stop the recording x min after the end of the program
        t.getEndTime().add(Calendar.MINUTE, bufferAfter);
        t.setPostBuffer(bufferAfter);
        CreateTimerAction cta = new CreateTimerAction(timerManager, t);
        cta.setCallback((cmd, response) -> {
            if (cmd.isSuccess()) {
                timerManager.assignProgramToTimer(selectedProgram, t);
            }
        });
        cta.enqueue();
    }

    @Override
    public String getMarkIconName() {
        return "tvhsync/tvhsync16.png";
    }

    @Override
    public PluginInfo getInfo() {
        String name = TVHsync.getTranslation("tvhsync", "TVHsync");
        String description = TVHsync.getTranslation("desc",
                "Manages the timers and recordings of a tvheadend server. Based on the plugin Lazy Bones by Henrik Niehaus.");
        String author = "Christian Echterling, based on Lazy Bones by Henrik Niehaus";
        return new PluginInfo(getClass(), name, description, author, "BSD", "https://github.com/Sagestum/tvhsync");
    }

    public static Version getVersion() {
        // the minor version has two digits: 0, 10, 0 is shown as 0.1.0
        return new Version(0, 10, 0, false);
    }

    public MainDialog getMainDialog() {
        if (mainDialog == null) {
            mainDialog = new MainDialog(getParent(), timerManager, recordingManager);
        }
        return mainDialog;
    }

    @Override
    public devplugin.SettingsTab getSettingsTab() {
        return new SettingsPanel(timerManager);
    }

    @Override
    public void onDeactivation() {
        String surviveOnExit = props.getProperty("surviveOnExit");
        if (Boolean.FALSE.toString().toLowerCase().equals(surviveOnExit)) {
            try {
                Player.stop();
            } catch (Exception e) {
                // fail silently, it's fine
            }
        }
    }

    @Override
    public void loadSettings(Properties props) { // NOSONAR
        TVHsync.props = props;

        // load data
        loadData();

        String url = props.getProperty("url");
        url = url == null ? "http://localhost:9981" : url;
        props.setProperty("url", url);
        String user = props.getProperty("user");
        user = user == null ? "" : user;
        props.setProperty("user", user);
        String password = props.getProperty("password");
        password = password == null ? "" : password;
        props.setProperty("password", password);
        String timeout = props.getProperty("timeout");
        timeout = timeout == null ? "5000" : timeout;
        props.setProperty("timeout", timeout);
        String streamurl = props.getProperty("streamurl");
        streamurl = streamurl == null ? "<url>/stream/channel/<channel>" : streamurl;
        props.setProperty("streamurl", streamurl);
        String threshold = props.getProperty("percentageThreshold");
        threshold = threshold == null ? "45" : threshold;
        props.setProperty("percentageThreshold", threshold);

        String timerBefore = props.getProperty("timer.before");
        timerBefore = timerBefore == null ? "5" : timerBefore;
        String timerAfter = props.getProperty("timer.after");
        timerAfter = timerAfter == null ? "10" : timerAfter;
        String timerPrio = props.getProperty("timer.prio");
        timerPrio = timerPrio == null ? Integer.toString(tvhsync.model.Timer.PRIO_DEFAULT) : timerPrio;
        String timerLifetime = props.getProperty("timer.lifetime");
        timerLifetime = timerLifetime == null ? "0" : timerLifetime;
        props.setProperty("timer.before", timerBefore);
        props.setProperty("timer.after", timerAfter);
        props.setProperty("timer.prio", timerPrio);
        props.setProperty("timer.lifetime", timerLifetime);

        String numberOfCards = props.getProperty("numberOfCards");
        numberOfCards = numberOfCards == null ? "1" : numberOfCards;
        props.setProperty("numberOfCards", numberOfCards);

        String surviveOnExit = props.getProperty("surviveOnExit");
        surviveOnExit = surviveOnExit == null ? "false" : surviveOnExit;
        props.setProperty("surviveOnExit", surviveOnExit);

        String recordingURL = props.getProperty("recording.url");
        recordingURL = recordingURL == null ? "<url>/dvrfile/<recording>" : recordingURL;
        props.setProperty("recording.url", recordingURL);

        String logConnectionErrors = props.getProperty("logConnectionErrors");
        logConnectionErrors = logConnectionErrors == null ? "true" : logConnectionErrors;
        props.setProperty("logConnectionErrors", logConnectionErrors);

        String logEPGErrors = props.getProperty("logEPGErrors");
        logEPGErrors = logEPGErrors == null ? "true" : logEPGErrors;
        props.setProperty("logEPGErrors", logEPGErrors);

        String showTimerOptionsDialog = props.getProperty("showTimerOptionsDialog");
        showTimerOptionsDialog = showTimerOptionsDialog == null ? "true" : showTimerOptionsDialog;
        props.setProperty("showTimerOptionsDialog", showTimerOptionsDialog);

        String descSourceTvb = props.getProperty("descSourceTvb");
        descSourceTvb = descSourceTvb == null ? DescriptionSelectorItem.LONGEST : descSourceTvb;
        props.setProperty("descSourceTvb", descSourceTvb);

        String seriesTitle = props.getProperty("timer.series.title");
        seriesTitle = seriesTitle == null ? SeriesTitleSelectorItem.TVH : seriesTitle;
        props.setProperty("timer.series.title", seriesTitle);

        String minChannelNumber = props.getProperty("minChannelNumber");
        minChannelNumber = minChannelNumber == null ? "0" : minChannelNumber;
        props.setProperty("minChannelNumber", minChannelNumber);
        String maxChannelNumber = props.getProperty("maxChannelNumber");
        maxChannelNumber = maxChannelNumber == null ? "0" : maxChannelNumber;
        props.setProperty("maxChannelNumber", maxChannelNumber);

        String timelineStartHour = props.getProperty("timelineStartHour");
        timelineStartHour = timelineStartHour == null ? "5" : timelineStartHour;
        props.setProperty("timelineStartHour", timelineStartHour);

        String markPriority = props.getProperty("markPriority");
        markPriority = markPriority == null ? "3" : markPriority;
        props.setProperty("markPriority", markPriority);

        TvhConnection.url = url;
        TvhConnection.user = user;
        TvhConnection.password = password;
        TvhConnection.timeout = Integer.parseInt(timeout);

        init();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        XStream xstream = new XStream();
        xstream.allowTypesByRegExp(new String[]{"tvhsync\\..*"});

        // load title mapping
        try {
            Map<String, String> titleMapping = (HashMap<String, String>) xstream.fromXML(props.getProperty("titleMapping"));
            timerManager.getTitleMapping().setMappingFromMap(titleMapping);
        } catch (Exception e) {
            logger.warn("Couldn't load title mapping", e);
        }

        // load channel mapping
        try {
            Map<String, Channel> channelMapping = (Hashtable<String, Channel>) xstream.fromXML(props.getProperty("channelMapping")); // NOSONAR this gets serialized, can't change the type
            ChannelManager.setChannelMapping(channelMapping);
        } catch (Exception e) {
            logger.warn("Couldn't load channel mapping", e);
        }

        // load timers
        try {
            List<TvhTimer> timers = (ArrayList<TvhTimer>) xstream.fromXML(props.getProperty("timers"));
            timerManager.setStoredTimers(timers);
        } catch (Exception e) {
            logger.warn("Couldn't load timers", e);
        }

        // load channel list
        try {
            List<Channel> channelList = (List<Channel>) xstream.fromXML(props.getProperty("channelList"));
            ChannelManager.getInstance().setChannels(channelList);
        } catch (Exception e) {
            logger.warn("Couldn't load channel list", e);
        }

        // remove outdated timers
        Calendar today = Calendar.getInstance();
        for (Iterator<TvhTimer> iter = timerManager.getStoredTimers().iterator(); iter.hasNext();) {
            TvhTimer timer = iter.next();
            if (timer.getEndTime().before(today)) {
                iter.remove();
            }
        }
    }

    @Override
    public void handleTvBrowserStartFinished() {
        // load channel list from tvheadend
        logger.debug("Updating channel list");
        ChannelManager.getInstance().update();

        // synchronize timers and recordings
        synchronize();
    }

    private void init() {
        instance = this;

        wrapper = new PluginCenterPanelWrapper() {
            //@formatter:off
            PluginCenterPanel[] panels =  new PluginCenterPanel[] {
                    new TimelineCenterPanel(timerManager),
                    new TimersCenterPanel(timerManager, recordingManager),
                    new RecordingsCenterPanel(recordingManager)/*,
                    new RemoteControlCenterPanel()*/
            };
            //@formatter:on

            @Override
            public PluginCenterPanel[] getCenterPanels() {
                return panels;
            }
        };

        // observe the timer list
        timerManager.addTimersChangedListener(this);

        // initialize logging
        initLogging();
    }

    private void initLogging() {
        String logDirectory = Settings.Directories.LOG.getString();
        if (logDirectory != null && System.getProperty("java.util.logging.config.file") == null) {
            // no logging config file is set, so we can adjust the logging level by ourselves
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            rootLogger.setLevel(Level.FINE);
            logger.info("No logging configuration defined. Setting logging level to Level.FINE");
        }

        // create our special logging components
        SimpleFormatter formatter = new SimpleFormatter();
        Handler eph = new PopupHandler();
        Handler dch = new DebugConsoleHandler();
        dch.setLevel(Level.FINEST);

        // add our special handlers to all tvhsync.* messages
        LoggerFactory.getLogger("tvhsync");
        java.util.logging.Logger lazyLogger = java.util.logging.Logger.getLogger("tvhsync");
        lazyLogger.addHandler(eph);
        lazyLogger.addHandler(dch);
        eph.setFormatter(formatter);

        // add our special handlers to the popuplogger
        LoggerFactory.getLogger(PopupHandler.KEYWORD);
        java.util.logging.Logger popupLogger = java.util.logging.Logger.getLogger(PopupHandler.KEYWORD);
        popupLogger.addHandler(eph);
        popupLogger.addHandler(dch);
        popupLogger.setLevel(Level.INFO);
        eph.setLevel(Level.INFO);

        // create a custom file handler only for TVHsync
        if (logDirectory != null) {
            Handler fh = null;
            try {
                fh = new FileHandler(new File(logDirectory, "tvhsync.log").getAbsolutePath());
                fh.setFormatter(formatter);
                fh.setLevel(Level.FINE);
            } catch (Exception e) {
                logger.warn("Couldn't add file handler for TVHsync", e);
            }

            if (fh != null) {
                lazyLogger.addHandler(fh);
                popupLogger.addHandler(fh);
            }
        }
    }

    @Override
    public Properties storeSettings() {
        storeData();
        return props;
    }

    private void storeData() {
        XStream xstream = new XStream();
        xstream.allowTypesByRegExp(new String[] {"tvhsync\\..*"});
        props.setProperty("channelMapping", xstream.toXML(ChannelManager.getChannelMapping()));
        props.setProperty("timers", xstream.toXML(timerManager.getTimers()));
        props.setProperty("titleMapping", xstream.toXML(timerManager.getTitleMapping().getAsMap()));
        props.setProperty("channelList", xstream.toXML(ChannelManager.getInstance().getChannels()));
    }

    public static Properties getProperties() {
        return props;
    }

    public Icon getIcon(String path) {
        return createImageIcon(path);
    }

    @Override
    public void handleTvDataUpdateFinished() {
        ProgramManager.getInstance().markPrograms(timerManager);
    }

    public Frame getParent() {
        return getParentFrame();
    }

    /**
     * Updates the pluginview of TV-Browser
     */
    public void updateTree() {
        PluginTreeNode node = getRootNode();
        node.removeAllActions();
        node.removeAllChildren();

        for (TvhTimer timer : timerManager.getTimers()) {
            if (timer.isAssigned()) {
                addProgramsToPluginTree(node, timer.getTvBrowserProgIDs());
            }
        }

        node.update();
    }

    private void addProgramsToPluginTree(PluginTreeNode node, List<String> tvBrowserProgIDs) {
        for (String progID : tvBrowserProgIDs) {
            Program prog = ProgramDatabase.getProgram(progID);
            if (prog != null) {
                node.addProgram(prog);
            } else { // can be null, if program time is near 00:00, because then
                // the wrong day is taken to ask tvb for the programm
                prog = ProgramDatabase.getProgram(progID);
                if (prog != null) {
                    node.addProgram(prog);
                }
            }
        }
    }

    @Override
    public boolean canUseProgramTree() {
        return true;
    }

    public static String getTranslation(String key, String altText) {
        return mLocalizer.msg(key, altText);
    }

    public static String getTranslation(String key, String altText, String arg1) {
        return mLocalizer.msg(key, altText, arg1);
    }

    public static String getTranslation(String key, String altText, String arg1, String arg2) {
        return mLocalizer.msg(key, altText, arg1, arg2);
    }

    public static String getTranslation(String key, String altText, String arg1, String arg2, String arg3) {
        return mLocalizer.msg(key, altText, arg1, arg2, arg3);
    }

    public JPopupMenu getSimpleContextMenu(TvhTimer timer) {
        return cmf.createSimpleActionMenu(timer);
    }

    @Override
    public void timersChanged(TimersChangedEvent evt) {
        updateTree();
    }

    public void synchronize() {
        timerManager.synchronize();
        recordingManager.synchronize();
    }

    private class ContextMenuFactory {
        public ActionMenu createChannelActionMenu(final devplugin.Channel chan) {
            final Channel tvhChan = ChannelManager.getChannelMapping().get(chan.getId());
            if (tvhChan == null || !Player.isConfigured()) {
                // selected channel is not mapped to a tvheadend channel or there is no player to watch it, so it does not make sense to show a menu at all
                return null;
            }

            AbstractAction watch = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    Player.play(tvhChan);
                }
            };
            watch.putValue(Action.NAME, TVHsync.getTranslation("watch", "Stream this channel live"));
            watch.putValue(Action.SMALL_ICON, createImageIcon("actions", "media-playback-start", 16));

            String name = TVHsync.getTranslation("tvhsync", "TVHsync");
            ImageIcon icon = createImageIcon("tvhsync/tvhsync16.png");
            return new ActionMenu(name, icon, new ActionMenu[] { new ActionMenu(watch) });
        }

        public ActionMenu createActionMenu(final Program program) {
            final Channel tvhChan = ChannelManager.getChannelMapping().get(program.getChannel().getId());
            if (tvhChan == null) {
                // selected channel is not mapped to a tvheadend channel, so it does not make sense to show a menu at all
                return null;
            }

            Marker[] markers = program.getMarkerArr();
            boolean marked = false;
            for (int i = 0; i < markers.length; i++) {
                if (markers[i].getId().equals(getId())) {
                    marked = true;
                    break;
                }
            }

            boolean notAssignedTimersExist = !timerManager.getNotAssignedTimers().isEmpty();

            int size = 4;

            if (marked || notAssignedTimersExist) {
                size++;
            }

            List<ActionMenu> actions = new ArrayList<>(size);
            // watching the live stream only makes sense, if an external player has been configured
            if (Player.isConfigured()) {
                AbstractAction watch = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        Player.play(program);
                    }
                };
                watch.putValue(Action.NAME, TVHsync.getTranslation("watch", "Stream this channel live"));
                watch.putValue(Action.SMALL_ICON, createImageIcon("actions", "media-playback-start", 16));
                actions.add(new ActionMenu(watch));
            }

            if (marked) {
                addActionMenuForMarkedProgram(actions, program);
            } else {
                addActionMenuForUnmarkedProgram(actions, program, notAssignedTimersExist);
            }

            String name = TVHsync.getTranslation("tvhsync", "TVHsync");
            ImageIcon icon = createImageIcon("tvhsync/tvhsync16.png");
            return new ActionMenu(name, icon, actions.toArray(new ActionMenu[0]));
        }

        private void addActionMenuForUnmarkedProgram(List<ActionMenu> actions, Program program, boolean notAssignedTimersExist) {
            AbstractAction action1 = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    timerManager.createTimer(program, false);
                }
            };
            action1.putValue(Action.NAME, TVHsync.getTranslation("capture", "Record with tvheadend"));
            action1.putValue(Action.SMALL_ICON, createImageIcon("tvhsync/capture.png"));
            actions.add(new ActionMenu(action1));

            AbstractAction action2 = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    TVHsync.getInstance().synchronize();
                }
            };
            action2.putValue(Action.NAME, TVHsync.getTranslation("resync", "Synchronize with tvheadend"));
            action2.putValue(Action.SMALL_ICON, createImageIcon("actions", "view-refresh", 16));
            actions.add(new ActionMenu(action2));

            if (notAssignedTimersExist) {
                Action[] timers = new Action[timerManager.getNotAssignedTimers().size()];
                List<TvhTimer> timerList = timerManager.getNotAssignedTimers();
                for (int i = 0; i < timers.length; i++) {
                    final TvhTimer timer = timerList.get(i);
                    timers[i] = new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ProgramManager.getInstance().assignTimerToProgram(program, timer);
                            timerManager.assignProgramToTimer(program, timer);
                            updateTree();
                        }
                    };
                    timers[i].putValue(Action.NAME, timerList.get(i).getDisplayTitle());
                    timers[i].putValue(TVHsync.TIMER_MENU_KEY, timerList.get(i));
                    timers[i].putValue(Action.SMALL_ICON, createImageIcon("tvhsync/appointment-new.png"));
                }
                String name = TVHsync.getTranslation("assign", "Assign");
                ImageIcon icon = createImageIcon("tvhsync/appointment-new.png");
                actions.add(new ActionMenu(name, icon, timers));
            }
        }

        private void addActionMenuForMarkedProgram(List<ActionMenu> actions, Program program) {
            AbstractAction action1 = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    timerManager.deleteTimer(program);
                }
            };
            action1.putValue(Action.NAME, TVHsync.getTranslation("dont_capture", "Delete timer"));
            action1.putValue(Action.SMALL_ICON, createImageIcon("actions", "edit-delete", 16));
            actions.add(new ActionMenu(action1));

            AbstractAction action2 = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    logger.info("Looking up timer for {}", program);
                    TvhTimer timer = timerManager.getTimer(program);
                    logger.info("Found timer {}", timer);
                    timerManager.editTimer(timer);
                }
            };
            action2.putValue(Action.NAME, TVHsync.getTranslation("edit", "Edit Timer"));
            action2.putValue(Action.SMALL_ICON, createImageIcon("actions", "document-edit", 16));
            actions.add(new ActionMenu(action2));

            AbstractAction action3 = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    TVHsync.getInstance().synchronize();
                }
            };
            action3.putValue(Action.NAME, TVHsync.getTranslation("resync", "Synchronize with tvheadend"));
            action3.putValue(Action.SMALL_ICON, createImageIcon("actions", "view-refresh", 16));
            actions.add(new ActionMenu(action3));
        }

        JPopupMenu simpleMenu;

        public JPopupMenu createSimpleActionMenu(final TvhTimer timer) {
            if (simpleMenu == null) {
                simpleMenu = new JPopupMenu();
                JMenuItem delItem = new JMenuItem(TVHsync.getTranslation("dont_capture", "Delete timer"), createImageIcon("actions", "edit-delete", 16));
                delItem.addActionListener(e -> timerManager.deleteTimer(timer));

                JMenuItem editItem = new JMenuItem(TVHsync.getTranslation("edit", "Edit Timer"), createImageIcon("actions", "document-edit", 16));
                editItem.addActionListener(e -> timerManager.editTimer(timer));

                simpleMenu.add(editItem);
                simpleMenu.add(delItem);
            }

            return simpleMenu;
        }

    }

    // ################ to receive Programs from other plugins ######################
    private static final String TARGET_WATCH = "watch";
    private static final String TARGET_CAPTURE = "capture";

    @Override
    public boolean canReceiveProgramsWithTarget() {
        return true;
    }

    @Override
    public ProgramReceiveTarget[] getProgramReceiveTargets() {
        ProgramReceiveTarget capture = new ProgramReceiveTarget(this, TVHsync.getTranslation("capture", "Record with tvheadend"), TARGET_CAPTURE);
        if (Player.isConfigured()) {
            return new ProgramReceiveTarget[] { capture, new ProgramReceiveTarget(this, TVHsync.getTranslation("watch", "Stream this channel live"), TARGET_WATCH) };
        }
        return new ProgramReceiveTarget[] { capture };
    }

    @Override
    public boolean receivePrograms(int eventType, Program[] programArr, ProgramReceiveTarget receiveTarget) {
        logger.debug("Program received for target [{}]", receiveTarget.getTargetId());
        if (TARGET_CAPTURE.equals(receiveTarget.getTargetId())) {
            // store current property values
            String threshold = props.getProperty("percentageThreshold");
            String showTimerOptionsDialog = props.getProperty("showTimerOptionsDialog");

            String logEpgErrors = props.getProperty("logEPGErrors");
            String logConnectionErrors = props.getProperty("logConnectionErrors");

            // set properties to "automatic mode"
            props.setProperty("percentageThreshold", "0");
            props.setProperty("showTimerOptionsDialog", "false");
            props.setProperty("logEPGErrors", "false");
            props.setProperty("logConnectionErrors", "false");

            // create timers for all programs
            for (int i = 0; i < programArr.length; i++) {
                Program prog = programArr[i];
                timerManager.createTimer(prog, true);
            }

            // restore old properties
            props.setProperty("percentageThreshold", threshold);
            props.setProperty("showTimerOptionsDialog", showTimerOptionsDialog);
            props.setProperty("logEPGErrors", logEpgErrors);
            props.setProperty("logConnectionErrors", logConnectionErrors);
        } else if (TARGET_WATCH.equals(receiveTarget.getTargetId())) {
            Player.play(programArr[0]);
        }
        return true;
    }

    @Override
    public int getMarkPriorityMaxForProgram(Program p) {
        TvhTimer timer = timerManager.getTimer(p);
        if (timer != null && !timer.isActive()) {
            return Program.PRIORITY_MARK_MIN;
        }
        return Integer.parseInt(props.getProperty("markPriority"));
    }
}
