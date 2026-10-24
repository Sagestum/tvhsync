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
package tvhsync;

import static devplugin.Plugin.getPluginManager;

import java.awt.Cursor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

import tvhsync.tvh.Response;
import tvhsync.model.Channel;
import tvhsync.model.EPGEntry;
import tvhsync.model.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import devplugin.Date;
import devplugin.Program;
import tvhsync.ChannelManager.ChannelNotFoundException;
import tvhsync.actions.CreateTimerAction;
import tvhsync.actions.DeleteTimerAction;
import tvhsync.actions.ModifyTimerAction;
import tvhsync.conflicts.Conflict;
import tvhsync.conflicts.ConflictFinder;
import tvhsync.conflicts.ConflictResolver;
import tvhsync.gui.components.timeroptions.TimerOptionsDialog;
import tvhsync.gui.timers.TimerSelectionDialog;
import tvhsync.logging.LoggingConstants;
import tvhsync.logging.PopupHandler;
import tvhsync.programmanager.ProgramDatabase;
import tvhsync.programmanager.ProgramManager;
import tvhsync.tvh.Tvheadend;
import tvhsync.utils.Utilities;

/**
 * Class to manage all timers.
 *
 * @author <a href="hampelratte@users.sf.net">hampelratte@users.sf.net</a>
 */
public class TimerManager {

    private static Logger logger = LoggerFactory.getLogger(TimerManager.class);
    private static Logger conLog = LoggerFactory.getLogger(LoggingConstants.CONNECTION_LOGGER);
    private static Logger epgLog = LoggerFactory.getLogger(LoggingConstants.EPG_LOGGER);
    private static Logger popupLog = LoggerFactory.getLogger(PopupHandler.KEYWORD);

    private static final int UNKNOWN = -1;

    /**
     * Stores all timers as Timer objects
     */
    private final List<TvhTimer> timers = new ArrayList<>();
    private final Lock timerListLock = new ReentrantLock();

    /**
     * The tvheadend timers from the last session, which have been stored to disk
     */
    private List<TvhTimer> storedTimers = new ArrayList<>();

    /**
     * Stores mappings the user has made for later use. The user has to map one Program only once. Later the mapping will be looked up here
     */
    private TitleMapping titleMapping = new TitleMapping();

    private static final Cursor WAITING_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

    private ConflictFinder conflictFinder = new ConflictFinder();

    private RecordingManager recordingManager;
    
    private List<TimersChangedListener> timersChangedListeners = new ArrayList<>();

    private void addTimer(TvhTimer timer, boolean notifyObservers) {
        timerListLock.lock();
        timers.add(timer);
        timerListLock.unlock();

        if (notifyObservers) {
            fireTimersChangedEvent(new TimersChangedEvent(TimersChangedEvent.TIMER_ADDED, timer));
        }
    }

    public void removeTimer(TvhTimer timer) {
        timerListLock.lock();
        timers.remove(timer);
        timerListLock.unlock();
        fireTimersChangedEvent(new TimersChangedEvent(TimersChangedEvent.TIMER_REMOVED, timer));
    }

    /**
     * @return a List of Timer objects
     */
    public List<TvhTimer> getTimers() {
        // return a copy of the timer list to avoid ConcurrentModificationExceptions
        timerListLock.lock();
        List<TvhTimer> timersCopy = new ArrayList<>(timers);
        timerListLock.unlock();
        return timersCopy;
    }

    /**
     * @param tvhTimers
     *            an List of Timer objects
     */
    public void setTimers(List<TvhTimer> tvhTimers) {
        for (TvhTimer timer : tvhTimers) {
            addTimer(new TvhTimer(timer), false);
        }

        // try to mark programs
        ProgramManager.getInstance().markPrograms(this);
        List<TvhTimer> notAssigned = getNotAssignedTimers();
        if (!notAssigned.isEmpty()) {
            handleNotAssignedTimers();
        }

        // handle conflicts, if some have been detected
        Set<Conflict> conflicts = conflictFinder.findConflictingTimers(getTimers());
        if (!conflicts.isEmpty()) {
            // clear old conflicts, which were stored on timers
            timerListLock.lock();
            for (TvhTimer timer : timers) {
                timer.getConflicts().clear();
            }
            timerListLock.unlock();

            // save conflicts on timers
            for (Conflict conflict : conflicts) {
                for (TvhTimer timer : conflict.getInvolvedTimers()) {
                    timer.getConflicts().add(conflict);
                }
            }

            boolean showTimerConflicts = Boolean.parseBoolean(TVHsync.getProperties().getProperty("timer.conflicts.show", "true"));
            if (showTimerConflicts) {
                ConflictResolver conflictResolver = new ConflictResolver(conflicts.iterator().next(), getTimers());
                conflictResolver.handleConflicts();
            }
        }

        // notify observers, that the timers have changed
        fireTimersChangedEvent(new TimersChangedEvent(TimersChangedEvent.ALL, getTimers()));
    }

    /**
     *
     * @param prog
     *            a devplugin.Program object
     * @return the timer for this program or null
     * @see Program
     */
    public TvhTimer getTimer(Program prog) {
        String progID = prog.getUniqueID();
        if (progID == null) {
            // this really should not happen
            logger.warn("Unique program ID is null. Unable to find a timer for this program [{}]", prog);
            return null;
        }

        timerListLock.lock();
        try {
            for (TvhTimer timer : timers) {
                List<String> tvBrowserProdIDs = timer.getTvBrowserProgIDs();
                for (String curProgID : tvBrowserProdIDs) {
                    if (progID.equals(curProgID)) {
                        return timer;
                    }
                }
            }
        } finally {
            timerListLock.unlock();
        }
        return null;
    }

    /**
     *
     * @param timerNumber
     *            The number of the timer
     * @return The timer with the specified number
     */
    public TvhTimer getTimer(String timerId) {
        timerListLock.lock();
        try {
            for (TvhTimer timer : timers) {
                if (timer.getID().equals(timerId)) {
                    return timer;
                }
            }
        } finally {
            timerListLock.unlock();
        }
        return null;
    }

    /**
     * Returns all timers, which couldn't be assigned to a program
     *
     * @return an ArrayList with Timer objects
     */
    public List<TvhTimer> getNotAssignedTimers() {
        ArrayList<TvhTimer> list = new ArrayList<>();
        timerListLock.lock();
        try {
            for (TvhTimer timer : timers) {
                if (!timer.isAssigned()) {
                    list.add(timer);
                }
            }
        } finally {
            timerListLock.unlock();
        }
        return list;
    }

    /**
     * For DEBUG only - print all timers to System.out
     *
     */
    public void printTimers() {
        System.out.println("########## Listing timers #################"); // NOSONAR
        for (TvhTimer timer : timers) {
            System.out.println(timer); // NOSONAR
        }
        System.out.println("################ End ######################"); // NOSONAR
    }

    public List<TvhTimer> getStoredTimers() {
        return storedTimers;
    }

    public void setStoredTimers(List<TvhTimer> storedTimers) {
        this.storedTimers = storedTimers;
    }

    /**
     * Checks storedTimers, if this timer has been mapped to Program before. In case it has been mapped before, we return a list of all the program ids, this
     * timer has been assigned to.
     *
     * @param timer
     * @return a list of program ids
     */
    public List<String> hasBeenMappedBefore(TvhTimer timer) {
        for (TvhTimer storedTimer : storedTimers) {
            if (timer.getUniqueKey().equals(storedTimer.getUniqueKey())) {
                if (storedTimer.getReason() == TvhTimer.NO_PROGRAM) {
                    return List.of("NO_PROGRAM");
                } else {
                    return storedTimer.getTvBrowserProgIDs();
                }
            }
        }
        return Collections.emptyList();
    }

    public void replaceStoredTimer(TvhTimer timer) {
        for (TvhTimer storedTimer : storedTimers) {
            if (timer.getUniqueKey().equals(storedTimer.getUniqueKey())) {
                storedTimers.remove(storedTimer);
                storedTimers.add(timer);
                return;
            }
        }

        // timer couldn't be found -> this is a new timer
        storedTimers.add(timer);
    }

    public TitleMapping getTitleMapping() {
        return this.titleMapping;
    }

    /**
     * Fetches the timer list from tvheadend
     */
    public synchronized void synchronize() {
        TVHsync.getInstance().getParent().setCursor(WAITING_CURSOR);
        TVHsync.getInstance().getMainDialog().setCursor(WAITING_CURSOR);

        // unmark all tvbrowser programs
        ProgramManager.getInstance().unmarkPrograms();

        // clear timer list
        timerListLock.lock();
        this.timers.clear();
        timerListLock.unlock();

        // fetch current timer list from tvheadend
        Response res = Tvheadend.listTimers();
        if (res.isSuccess()) {
            logger.info("Timers retrieved from tvheadend");
            List<Timer> tvhTimers = Tvheadend.parseTimers(res);
            List<TvhTimer> updatedTimers = new ArrayList<>();
            for (Timer tvhTimer : tvhTimers) {
                updatedTimers.add(new TvhTimer(tvhTimer));
            }
            setTimers(updatedTimers);

            // update recording list if necessary
            boolean updateRecordings = false;
            for (Timer timer : tvhTimers) {
                if (timer.isRecording()) {
                    updateRecordings = true;
                    break;
                }
            }
            if (updateRecordings) {
                recordingManager.synchronize();
            }
        } else {
            // something went wrong, we have no timers -> load the stored ones
            conLog.error(TVHsync.getTranslation("using_stored_timers", "Couldn't retrieve timers from tvheadend, using stored ones."));
            List<TvhTimer> storedTimers = getStoredTimers();
            setTimers(storedTimers);
        }

        TVHsync.getInstance().getParent().setCursor(DEFAULT_CURSOR);
        TVHsync.getInstance().getMainDialog().setCursor(DEFAULT_CURSOR);
    }

    /**
     * Deletes a timer on the tvheadend server
     *
     * @param timer
     *            timer to delete
     */
    public void deleteTimer(final TvhTimer timer) {
        deleteTimer(timer, null);
    }

    /**
     * Deletes a timer on the tvheadend server
     *
     * @param timer
     *            timer to delete
     * @param callback
     *            a Runnable object, which is run after the delete process is finished
     */
    public void deleteTimer(final TvhTimer timer, final Runnable callback) {
        DeleteTimerAction dta = new DeleteTimerAction(timer, (cmd, response) -> {
		    if (!cmd.isSuccess()) {
		        logger.error(TVHsync.getTranslation("couldnt_delete", "Couldn't delete timer:") + " " + cmd.getResponse().getMessage()); // NOSONAR
		    } else {
		        synchronize();
		    }

		    if (callback != null) {
		        callback.run();
		    }
		});
        dta.enqueue();
    }

    public void createTimerFromScratch() throws ChannelNotFoundException {
        TvhTimer timer = new TvhTimer();
        List<Channel> channels = ChannelManager.getInstance().getChannels();
        if (channels != null && !channels.isEmpty()) {
            timer.setChannelId(channels.get(0).getId());
        }
        Program prog = ProgramDatabase.getProgram(timer);

        // in this situation it makes sense to show the timer options
        // so we override the user setting (hide options dialog)
        boolean showTimerOptions = Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("showTimerOptionsDialog"));
        TVHsync.getProperties().setProperty("showTimerOptionsDialog", Boolean.TRUE.toString());
        createTimer(prog, false);
        TVHsync.getProperties().setProperty("showTimerOptionsDialog", Boolean.toString(showTimerOptions));
    }

    /**
     * Creates a new timer on the tvheadend server
     *
     * @param prog
     *            the Program to create a timer for
     * @param automatic
     *            supresses all user interaction
     */
    public void createTimer(Program prog, boolean automatic) {
        if (prog.isExpired()) {
            if (!automatic) {
                logger.error(TVHsync.getTranslation("expired", "This program has expired"));
            }
            return;
        }

        Channel c = ChannelManager.getChannelMapping().get(prog.getChannel().getId());
        if (c == null) {
            logger.error(TVHsync.getTranslation("no_channel_defined", "No channel defined", prog.toString()));
            return;
        }
        String id = c.getId();

        long middleOfProgramTimeInMillis = determineMiddleOfProgramTime(prog);
        Response res = Tvheadend.listEpgEventsAt(id, middleOfProgramTimeInMillis);

        if (res.isSuccess()) {
            List<EPGEntry> epgList = Tvheadend.parseEpgEvents(res);

            if (epgList.isEmpty()) {
                noEPGAvailable(prog, id, automatic);
                return;
            }

            EPGEntry tvhEPG = epgList.get(0);

            TvhTimer timer = new TvhTimer();
            timer.setChannelId(id);
            timer.setEventId(tvhEPG.getEventID());
            timer.addTvBrowserProgID(prog.getUniqueID());
            setStartAndEndTime(tvhEPG, timer);
            timer.setTitle(tvhEPG.getTitle());
            timer.createTimerDescription(prog, tvhEPG);

            boolean showOptionsDialog = !automatic && Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("showTimerOptionsDialog"));

            if (showOptionsDialog) {
                TimerOptionsDialog tod = new TimerOptionsDialog(this, recordingManager, timer, prog, TimerOptionsDialog.Mode.NEW);
                if (tod.isAccepted()) {
                    commitTimer(tod.getTimer(), tod.getOldTimer(), tod.getProgram(), false, false);
                }
            } else {
                commitTimer(timer, null, prog, false, automatic);
            }
        } else {
            if (res.getCode() == 0) {
                conLog.error(TVHsync.getTranslation("couldnt_create", "Couldn\'t create timer\n: ") + " " + res.getMessage()); // NOSONAR
            } else {
                logger.error(TVHsync.getTranslation("couldnt_create", "Couldn\'t create timer\n: ") + " " + res.getMessage()); // NOSONAR
            }
        }
    }

    private long determineMiddleOfProgramTime(Program prog) {
        Calendar cal = Calendar.getInstance();
        Date date = prog.getDate();
        cal.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        cal.set(Calendar.MONTH, date.getMonth() - 1);
        cal.set(Calendar.YEAR, date.getYear());
        cal.set(Calendar.HOUR_OF_DAY, prog.getHours());
        cal.set(Calendar.MINUTE, prog.getMinutes());
        if (prog.getLength() == UNKNOWN) {
            // if the length is unknown, we add 5 minutes to the starttime, so that we still get the right program,
            // if the tvheadend start time differs from the tvbrowser start time
            cal.add(Calendar.MINUTE, 5);
        } else {
            cal.add(Calendar.MINUTE, prog.getLength() / 2);
        }
        return cal.getTimeInMillis();
    }

    private void setStartAndEndTime(EPGEntry tvhEPG, TvhTimer timer) {
        // set start and end time
        Calendar calStart = (Calendar) tvhEPG.getStartTime().clone();
        timer.setStartTime(calStart);
        Calendar calEnd = (Calendar) tvhEPG.getEndTime().clone();
        timer.setEndTime(calEnd);
        setTimerBuffers(timer);
    }

    public static void setTimerBuffers(TvhTimer timer) {
        Calendar calStart = timer.getStartTime();
        Calendar calEnd = timer.getEndTime();

        // start the recording x min before the beginning of the program
        int bufferBefore = Integer.parseInt(TVHsync.getProperties().getProperty("timer.before"));
        calStart.add(Calendar.MINUTE, -bufferBefore);
        timer.setPreBuffer(bufferBefore);

        // stop the recording x min after the end of the program
        int bufferAfter = Integer.parseInt(TVHsync.getProperties().getProperty("timer.after"));
        calEnd.add(Calendar.MINUTE, bufferAfter);
        timer.setPostBuffer(bufferAfter);
    }

    private void noEPGAvailable(Program prog, String channelId, boolean automatic) {
        int bufferBefore = Integer.parseInt(TVHsync.getProperties().getProperty("timer.before"));
        int bufferAfter = Integer.parseInt(TVHsync.getProperties().getProperty("timer.after"));

        boolean dontCare = automatic || Boolean.FALSE.toString().equals(TVHsync.getProperties().getProperty("logEPGErrors"));
        int result = JOptionPane.NO_OPTION;
        if (!dontCare) {
            result = JOptionPane.showConfirmDialog(null, TVHsync.getTranslation("noEPGdata", ""), "", JOptionPane.YES_NO_OPTION);
        }
        if (dontCare || result == JOptionPane.OK_OPTION) {
            TvhTimer newTimer = new TvhTimer();
            newTimer.setState(Timer.ACTIVE);
            newTimer.setChannelId(channelId);
            int prio = Integer.parseInt(TVHsync.getProperties().getProperty("timer.prio"));
            int lifetime = Integer.parseInt(TVHsync.getProperties().getProperty("timer.lifetime"));
            newTimer.setLifetime(lifetime);
            newTimer.setPriority(prio);
            newTimer.setTitle(prog.getTitle());
            newTimer.addTvBrowserProgID(prog.getUniqueID());
            String descriptionSelectorItemId = TVHsync.getProperties().getProperty("descSourceTvb");
            String desc = TvhTimer.createDescription(descriptionSelectorItemId, "", prog);
            newTimer.setDescription(desc);

            Calendar startTime = prog.getDate().getCalendar();
            int start = prog.getStartTime();
            int hour = start / 60;
            int minute = start % 60;
            startTime.set(Calendar.HOUR_OF_DAY, hour);
            startTime.set(Calendar.MINUTE, minute);

            Calendar endTime = (Calendar) startTime.clone();
            endTime.add(Calendar.MINUTE, prog.getLength());

            // add buffers
            startTime.add(Calendar.MINUTE, -bufferBefore);
            newTimer.setStartTime(startTime);
            newTimer.setPreBuffer(bufferBefore);
            endTime.add(Calendar.MINUTE, bufferAfter);
            newTimer.setEndTime(endTime);
            newTimer.setPostBuffer(bufferAfter);

            if (automatic) {
                commitTimer(newTimer, null, prog, false, true);
            } else {
                TimerOptionsDialog tod = new TimerOptionsDialog(this, recordingManager, newTimer, prog, TimerOptionsDialog.Mode.NEW);
                if (tod.isAccepted()) {
                    commitTimer(tod.getTimer(), tod.getOldTimer(), tod.getProgram(), false, false);
                }
            }
        }
    }

    /**
     * Commits a new or changed timer to tvheadend
     *
     * @param timer
     *            The new created / updated Timer
     * @param oldTimer
     *            A clone of the timer with the old settings. Can be null for new timers.
     * @param prog
     *            The according Program
     * @param update
     *            If the Timer is a new one or if the timer has been edited
     * @param automatic
     *            Supresses all user interaction
     */
    private void commitTimer(final TvhTimer timer, TvhTimer oldTimer, final Program prog, boolean update, boolean automatic) {
        logger.debug("Comitting timer to tvheadend");
        String id = "";
        if (prog != null) {
            Channel chan = ChannelManager.getChannelMapping().get(prog.getChannel().getId());
            if (chan == null) {
                logger.error(TVHsync.getTranslation("no_channel_defined", "No channel defined", prog.toString()));
                return;
            }
            id = chan.getId();
        }

        if (update) {
        	modifyTimer(oldTimer, timer);
        } else {
            createNewTimer(prog, timer, automatic, id);
        }
    }

    private void createNewTimer(Program prog, TvhTimer timer, boolean automatic, String id) {
    	logger.debug("Creating a new timer");
        if (timer.getTitle() != null && prog != null) {
            int percentage = determineSimilarityBetween(prog, timer, automatic);
            int threshold = Integer.parseInt(TVHsync.getProperties().getProperty("percentageThreshold"));
            if (percentage > threshold) {
                CreateTimerAction cta = new CreateTimerAction(this, timer);
                cta.enqueue();
            } else {
                logger.debug("Looking in title mapping for timer {}", timer);
                // lookup in mapping history
                String timerTitle = getTitleMapping().getTvhTitle(prog.getTitle());
                if (timer.getTitle().equals(timerTitle)) {
                    TvhCallback<CreateTimerAction> callback = (cmd, response) -> {
					    if (cmd.isSuccess()) {
					        timer.addTvBrowserProgID(prog.getUniqueID());
					        replaceStoredTimer(timer);
					    }
					};
                    CreateTimerAction cta = new CreateTimerAction(this, timer);
                    cta.setCallback(callback);
                    cta.enqueue();
                } else { // no mapping found -> ask the user
                    showTimerConfirmDialog(timer, prog);
                }
            }
        } else { // tvheadend has no EPG data
            noEPGAvailable(prog, id, automatic);
        }
	}

	private int determineSimilarityBetween(Program prog, TvhTimer timer, boolean automatic) {
		int percentage;
		percentage = Utilities.percentageOfEquality(prog.getTitle(), timer.getTitle());
        if (automatic) {
            percentage = 100;
        }
        return percentage;
	}

	private void modifyTimer(TvhTimer oldTimer, TvhTimer newTimer) {
    	logger.debug("Timer exists and will be modified");
        TvhCallback<ModifyTimerAction> callback = (cmd, response) -> {
		    TimerManager.this.synchronize();
		    if (!cmd.isSuccess()) {
		        String mesg = TVHsync.getTranslation("couldnt_change", "Couldn\'t change timer:") + " " + cmd.getResponse().getMessage();
		        logger.error(mesg);
		    }
		};
        ModifyTimerAction mta = new ModifyTimerAction(newTimer, oldTimer);
        mta.setCallback(callback);
        mta.enqueue();
	}

	public void assignProgramToTimer(Program prog, TvhTimer timer) {
        timer.addTvBrowserProgID(prog.getUniqueID());
        replaceStoredTimer(timer);
        if (!prog.getTitle().equals(timer.getTitle())) {
            getTitleMapping().put(prog.getTitle(), timer.getTitle());
        }
    }

    /**
     * If a Program can't be assigned to a tvheadend program, this method shows a dialog to select the right tvheadend program
     *
     * @param prog
     *            the Program selected in TV-Browser
     * @param timerOptions
     *            the timer from TimerOptionsDialog
     */
    private void showTimerConfirmDialog(TvhTimer timerOptions, Program prog) {
        // get all programs 2 hours before and after the given program
        Calendar cal = Calendar.getInstance();
        Date date = prog.getDate();
        cal.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        cal.set(Calendar.MONTH, date.getMonth() - 1);
        cal.set(Calendar.YEAR, date.getYear());
        cal.set(Calendar.HOUR_OF_DAY, prog.getHours());
        cal.set(Calendar.MINUTE, prog.getMinutes());
        cal.add(Calendar.MINUTE, prog.getLength() / 2);

        devplugin.Channel chan = prog.getChannel();
        TreeSet<TvhTimer> programSet = new TreeSet<>();

        // get the program for the timer's time
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(cal.getTimeInMillis());
        TvhTimer t = ProgramManager.getInstance().getTimerForTime(c, chan);
        if (t != null) {
            programSet.add(t);
        }

        for (int i = 10; i <= 120; i += 10) {
            // get the program before the given one
            c = Calendar.getInstance();
            c.setTimeInMillis(cal.getTimeInMillis());
            c.add(Calendar.MINUTE, i * -1);
            TvhTimer t1 = ProgramManager.getInstance().getTimerForTime(c, chan);
            if (t1 != null) {
                programSet.add(t1);
            }

            // get the program after the given one
            c = Calendar.getInstance();
            c.setTimeInMillis(cal.getTimeInMillis());
            c.add(Calendar.MINUTE, i);
            TvhTimer t2 = ProgramManager.getInstance().getTimerForTime(c, chan);
            if (t2 != null) {
                programSet.add(t2);
            }
        }

        Program[] programs = new Program[programSet.size()];
        int i = 0;
        for (TvhTimer timer : programSet) {
            Calendar time = timer.getStartTime();
            TimerProgram p = new TimerProgram(chan, new Date(time), time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE));
            p.setTitle(timer.getTitle());
            p.setDescription("");
            p.setTimer(timer);
            programs[i++] = p;
        }

        // reverse the order of the programs
        Program[] temp = new Program[programs.length];
        for (int j = 0; j < programs.length; j++) {
            temp[j] = programs[programs.length - 1 - j];
        }
        programs = temp;

        // show dialog
        new TimerSelectionDialog(programs, timerOptions);

        TVHsync.getInstance().synchronize();
    }

    public void deleteTimer(final Program prog) {
        TvhTimer timer = getTimer(prog);
        logger.debug("Deleting timer {}", timer);
        TvhCallback<DeleteTimerAction> callback = (cmd, response) -> {
		    if (cmd instanceof DeleteTimerAction) {
		        if (!cmd.isSuccess()) {
		            logger.error(TVHsync.getTranslation("couldnt_delete", "Couldn\'t delete timer:") + " " + cmd.getResponse().getMessage()); // NOSONAR
		            return;
		        }

		        prog.unmark(TVHsync.getInstance());
		        synchronize();
		    }
		};
        DeleteTimerAction dta = new DeleteTimerAction(timer, callback);
        dta.enqueue();
    }

    public void editTimer(TvhTimer timer) {
        logger.debug("Looking up program for timer {}", timer);
        Program prog = null;
        if (!timer.getTvBrowserProgIDs().isEmpty()) {
            logger.debug("Timer has {} assigned programs", timer.getTvBrowserProgIDs().size());
            prog = ProgramDatabase.getProgram(timer.getTvBrowserProgIDs().get(0));
        } else {
            logger.warn("Timer has no program IDs assigned.");
        }
        logger.debug("Creating timer options dialog");
        TimerOptionsDialog tod = new TimerOptionsDialog(this, recordingManager, timer, prog, TimerOptionsDialog.Mode.UPDATE);
        if (tod.isAccepted()) {
            logger.debug("Timer options dialog has been accepted");
            commitTimer(tod.getTimer(), tod.getOldTimer(), tod.getProgram(), true, false);
        } else {
            logger.debug("Timer options dialog has been canceled");
        }
    }

    public boolean lookUpTimer(TvhTimer timer, Program candidate) {
        logger.debug("Looking in storedTimers for: {}", timer);
        boolean found = lookupMappedTimer(timer);

        if(!found) {
            logger.debug("No mapping found for: {}", timer);
            if (candidate != null) {
                logger.debug("Looking up old mappings");
                String progTitle = getTitleMapping().getTvbTitle(timer.getTitle());
                if (candidate.getTitle().equals(progTitle)) {
                    candidate.mark(TVHsync.getInstance()); // wieso mark hier drin? lookup hört sich nicht danach an
                    timer.addTvBrowserProgID(candidate.getUniqueID());
                    logger.debug("Old mapping found for: {}", timer);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean lookupMappedTimer(TvhTimer timer) {
        List<String> progIDs = hasBeenMappedBefore(timer);
        if (progIDs != null) { // we have a mapping of this timer to a program
            for (String progID : progIDs) {
                if (progID.equals("NO_PROGRAM")) {
                    logger.debug("Timer {} should never be assigned", timer);
                    timer.setReason(TvhTimer.NO_PROGRAM);
                    return true;
                } else {
                    return lookupMappedTimer(timer, progIDs, progID);
                }
            }
        }
        return false;
    }
    
    private boolean lookupMappedTimer(TvhTimer timer, List<String> progIDs, String progID) {
    	try {
            devplugin.Channel c = ChannelManager.getInstance().getTvbrowserChannel(timer);
            Date date = new Date(timer.getStartTime());
            Iterator<Program> iterator = getPluginManager().getChannelDayProgram(date, c);
            while (iterator != null && iterator.hasNext()) {
                Program p = iterator.next();
                if (p.getUniqueID().equals(progID) && p.getDate().equals(date)) {
                    p.mark(TVHsync.getInstance());
                    timer.setTvBrowserProgIDs(progIDs);
                    logger.debug("Mapping found for: {}", timer);
                    return true;
                }
            }
        } catch (ChannelNotFoundException e) {
            // fail silently
        }
    	return false;
    }

    /**
     * Handles all timers, which couldn't be assigned automatically
     *
     */
    public void handleNotAssignedTimers() {
        if (Boolean.TRUE.toString().equals(TVHsync.getProperties().getProperty("supressMatchDialog"))) {
            return;
        }
        Iterator<TvhTimer> iterator = getNotAssignedTimers().iterator();
        logger.debug("Not assigned timers: {}", getNotAssignedTimers().size());
        while (iterator.hasNext()) {
            TvhTimer timer = iterator.next();
            switch (timer.getReason()) {
            case TvhTimer.NOT_FOUND:
                // show message
                java.util.Date date = new java.util.Date(timer.getStartTime().getTimeInMillis());
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                String dateString = sdf.format(date);
                String title = timer.getTitle();
                Channel chan = ChannelManager.getInstance().getChannelById(timer.getChannelId());
                String channelName = "unknown channel";
                if (chan != null) {
                    channelName = chan.getName();
                }
                String msg = TVHsync.getTranslation("message_programselect",
                        "I couldn\'t find a program, which matches the tvheadend timer\n<b>{0}</b> at <b>{1}</b> on <b>{2}</b>.\n"
                                + "You may assign this timer to a program in the context menu.", title, dateString, channelName);
                popupLog.warn(msg);
                break;
            case TvhTimer.NO_EPG:
                logger.warn("Couldn't assign timer: {}", timer);
                String mesg = TVHsync.getTranslation("noEPGdataTVB",
                        "<html>TV-Browser has no EPG-data the timer {0}.<br>Please update your EPG-data!</html>", timer.toString());
                epgLog.error(mesg);
                break;
            case TvhTimer.NO_CHANNEL:
                mesg = TVHsync.getTranslation("no_channel_defined", "No channel defined", timer.toString());
                epgLog.error(mesg);
                break;
            case TvhTimer.NO_PROGRAM:
                // do nothing
                break;
            default:
                logger.debug("Not assigned timer: {}", timer);
            }
        }
    }

    public RecordingManager getRecordingManager() {
        return recordingManager;
    }

    public void setRecordingManager(RecordingManager recordingManager) {
        this.recordingManager = recordingManager;
    }
    
    public void addTimersChangedListener(TimersChangedListener l) {
    	this.timersChangedListeners.add(l);
    }
    
    private void fireTimersChangedEvent(TimersChangedEvent evt) {
    	for (TimersChangedListener timersChangedListener : timersChangedListeners) {
			timersChangedListener.timersChanged(evt);
		}
    }
}