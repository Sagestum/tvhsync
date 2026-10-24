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
package tvhsync.programmanager;

import static devplugin.Plugin.getPluginManager;

import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.swing.JPopupMenu;

import tvhsync.ChannelManager;
import tvhsync.ChannelManager.ChannelNotFoundException;
import tvhsync.TVHsync;
import tvhsync.TvhTimer;
import tvhsync.TimerManager;
import tvhsync.programmanager.evaluation.DoppelpackDetector;
import tvhsync.programmanager.evaluation.Evaluator;
import tvhsync.programmanager.evaluation.Result;

import tvhsync.model.Channel;
import tvhsync.model.EPGEntry;
import tvhsync.model.Timer;
import tvhsync.tvh.Response;
import tvhsync.tvh.Tvheadend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.programmouseevent.ProgramMouseEventHandler;
import devplugin.Program;

public class ProgramManager {
    private static Logger logger = LoggerFactory.getLogger(ProgramManager.class);

    private static ProgramManager instance;

    private final Evaluator evaluator = new Evaluator();

    private ProgramManager() {
    }

    public static ProgramManager getInstance() {
        if (instance == null) {
            instance = new ProgramManager();
        }
        return instance;
    }

    /**
     * Looks up the program of tvheadend, which is running at the given time on the given channel
     *
     * @return a timer for the program or null, if tvheadend has no EPG data for this time
     */
    public TvhTimer getTimerForTime(Calendar cal, devplugin.Channel chan) {
        Channel tvhChannel = ChannelManager.getChannelMapping().get(chan.getId());
        if (tvhChannel == null) {
            return null;
        }

        Response res = Tvheadend.listEpgEventsAt(tvhChannel.getId(), cal.getTimeInMillis());
        if (res.isSuccess()) {
            List<EPGEntry> epg = Tvheadend.parseEpgEvents(res);
            if (!epg.isEmpty()) {
                EPGEntry entry = epg.get(0);
                Timer timer = new Timer();
                timer.setChannelId(tvhChannel.getId());
                timer.setTitle(entry.getTitle());
                timer.setStartTime(entry.getStartTime());
                timer.setEndTime(entry.getEndTime());
                timer.setDescription(entry.getDescription());
                int prio = Integer.parseInt(TVHsync.getProperties().getProperty("timer.prio"));
                int lifetime = Integer.parseInt(TVHsync.getProperties().getProperty("timer.lifetime"));
                timer.setLifetime(lifetime);
                timer.setPriority(prio);
                timer.setDvrConfig(TVHsync.getProperties().getProperty("timer.dvrConfig", ""));
                TvhTimer tvhTimer = new TvhTimer(timer);
                tvhTimer.setEventId(entry.getEventID());
                return tvhTimer;
            }
        }
        return null;
    }

    public JPopupMenu getContextMenuForTimer(TvhTimer timer) {
        List<String> tvBrowserProgIds = timer.getTvBrowserProgIDs();
        JPopupMenu popup;
        if (!tvBrowserProgIds.isEmpty()) {
            Program prog = ProgramDatabase.getProgram(tvBrowserProgIds.get(0));
            popup = getPluginManager().createPluginContextMenu(prog, null);
        } else {
            popup = TVHsync.getInstance().getSimpleContextMenu(timer);
        }
        return popup;
    }

    /**
     * Called to mark all Programs
     */
    public void markPrograms(TimerManager timerManager) {
        List<TvhTimer> timers = timerManager.getTimers();
        // for every timer
        try {
            for (TvhTimer timer : timers) {
                markSingularTimer(timer, timerManager);
            }
        } catch (ConcurrentModificationException e) {
            // the timers list has changed while we were marking programs, let's start over
            logger.debug("Timers list has changed while marking programs. Beginning from scratch.");

            // first remove all previously set marks
            unmarkPrograms();

            // now mark all timers
            markPrograms(timerManager);
        }
    }

    /**
     * Unmarks all programs, which are marked by TVHsync
     */
    public void unmarkPrograms() {
        Program[] markedPrograms = getPluginManager().getMarkedPrograms();
        for (Program marked : markedPrograms) {
            marked.unmark(TVHsync.getInstance());
        }
    }



    /**
     *
     * @param timer
     * @param chan
     */
    private void markSingularTimer(TvhTimer timer, TimerManager timerManager) {
        // get the day program of the day, the previous day and the next day
        List<Program> threeDayProgram;
        try {
            threeDayProgram = ProgramDatabase.getProgramAroundTimer(timer);
        } catch (ChannelNotFoundException e) {
            timer.setReason(TvhTimer.NO_CHANNEL);
            return;
        }

        // Mark doppelpack timers. If we found a doppelpack, we can stop at this point
        boolean doppelpackFound = new DoppelpackDetector(threeDayProgram).markDoppelpackTimer(timer);
        if (doppelpackFound) {
            return;
        }

        // no doppelpacks, we can now evaluate the programs with
        // several criteria
        Result bestMatching = evaluator.evaluate(threeDayProgram, timer);
        if (bestMatching == null) {
            logger.warn("Couldn't assign timer: {}", timer);
            timer.setReason(TvhTimer.NOT_FOUND);
            return;
        }

        logger.debug("Best matching program for timer \"{}\" is \"{}\" with a percentage of {}", new Object[] { timer.getTitle(),
                bestMatching.getProgram().getTitle(), bestMatching.getPercentage() });
        int threshold = Integer.parseInt(TVHsync.getProperties().getProperty("percentageThreshold"));
        // if the percentage of equality is higher than the config value
        // percentageThreshold, mark this program
        if (bestMatching.getPercentage() >= threshold) {
            assignTimerToProgram(bestMatching.getProgram(), timer);
        } else {
            // no candidate and no doppelpack found
            // look for the timer in stored timers
            boolean found = timerManager.lookUpTimer(timer, bestMatching.getProgram());
            if (!found) { // we have no mapping
                logger.warn("Couldn't find a program with that title: {}", timer.getTitle());
                logger.warn("Couldn't assign timer: {}", timer);
                timer.setReason(TvhTimer.NOT_FOUND);
            }
        }
    }

    public void assignTimerToProgram(Program prog, TvhTimer timer) {
        timer.addTvBrowserProgID(prog.getUniqueID());
        prog.mark(TVHsync.getInstance());
        prog.validateMarking();
    }

    public void handleTimerDoubleClick(TvhTimer timer, MouseEvent e) {
        List<String> progIDs = timer.getTvBrowserProgIDs();
        if (!progIDs.isEmpty()) {
            String firstProgID = progIDs.get(0);
            Program prog = ProgramDatabase.getProgram(firstProgID);
            if (prog != null) {
                ProgramMouseEventHandler.handleProgramClick(prog, TVHsync.getInstance(), false, e);
            }
        }
    }
}
