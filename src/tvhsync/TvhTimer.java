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

import devplugin.Program;
import tvhsync.conflicts.Conflict;
import tvhsync.gui.settings.DescriptionSelectorItem;
import tvhsync.model.EPGEntry;
import tvhsync.model.Timer;
import util.paramhandler.ParamParser;
import util.program.AbstractPluginProgramFormating;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static devplugin.Plugin.getPluginManager;

public class TvhTimer extends Timer {

    private static final long serialVersionUID = 3L;

    public static final int NO_REASON = 0;
    public static final int NO_EPG = 1;
    public static final int NOT_FOUND = 2;
    public static final int NO_CHANNEL = 3;
    public static final int NO_PROGRAM = 4;

    /**
     * The reason, why this Timer couldn't be assigned
     */
    private int reason = TvhTimer.NO_REASON;

    /**
     * List of all TV-B programs assigned to this timer
     */
    private List<String> tvBrowserProgIDs = new ArrayList<>();

    /**
     * Contains all conflicts with other timers
     */
    private final List<Conflict> conflicts = new ArrayList<>();

    /**
     * The tvheadend EPG event, this timer has been created for. Only used to create new timers.
     */
    private transient long eventId;

    public TvhTimer() {
        setDefaultLifetimeAndPrio();
    }

    public TvhTimer(Timer timer) {
        timer.copyTo(this);
    }

    private void setDefaultLifetimeAndPrio() {
        int prio = Integer.parseInt(TVHsync.getProperties().getProperty("timer.prio"));
        setPriority(prio);
        int lifetime = Integer.parseInt(TVHsync.getProperties().getProperty("timer.lifetime"));
        setLifetime(lifetime);
        setDvrConfig(TVHsync.getProperties().getProperty("timer.dvrConfig", ""));
    }

    /**
     * Returns the IDs of all TV-Browser programs, which are assigned to this timer
     *
     * @return List of IDs of all TV-Browser programs, which are assigned to this timer
     */
    public List<String> getTvBrowserProgIDs() {
        return tvBrowserProgIDs;
    }

    public void setTvBrowserProgIDs(List<String> tvBrowserProgIDs) {
        this.tvBrowserProgIDs = tvBrowserProgIDs;
    }

    /**
     * @return Returns if this timer could be assigned to a Program
     */
    public boolean isAssigned() {
        return !tvBrowserProgIDs.isEmpty();
    }

    @Override
    public Object clone() { // NOSONAR java:S2975
        TvhTimer clone = new TvhTimer((Timer) super.clone());
        clone.setTvBrowserProgIDs(new ArrayList<>(getTvBrowserProgIDs()));
        clone.setEventId(getEventId());
        clone.setReason(getReason());
        return clone;
    }

    /**
     * @return The reason, why this timer couldn't be assigned
     */
    public int getReason() {
        return reason;
    }

    /**
     * Set the reason why this timer couldn't be assigned
     *
     * @param reason the reason why this timer couldn't be assigned
     * @see #NO_CHANNEL
     * @see #NO_EPG
     * @see #NO_PROGRAM
     * @see #NO_REASON
     * @see #NOT_FOUND
     */
    public void setReason(int reason) {
        this.reason = reason;
    }

    /**
     * @param id program ID from TV-Browser
     */
    public void addTvBrowserProgID(String id) {
        tvBrowserProgIDs.add(id);
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }

    public String getDisplayTitle() {
        return getTitle();
    }

    /**
     * @return This timer without time buffers
     */
    public TvhTimer getTimerWithoutBuffers() {
        TvhTimer timer = (TvhTimer) this.clone();
        timer.getStartTime().add(Calendar.MINUTE, getPreBuffer());
        timer.getEndTime().add(Calendar.MINUTE, -getPostBuffer());
        timer.setPreBuffer(0);
        timer.setPostBuffer(0);
        return timer;
    }

    public void createTimerDescription(Program prog, EPGEntry tvhEPG) {
        setDescription(tvhEPG.getDescription());
        String descTvh = getDescription() == null ? "" : getDescription();
        String descriptionSelectorItemId = TVHsync.getProperties().getProperty("descSourceTvb");
        String description = createDescription(descriptionSelectorItemId, descTvh, prog);
        setDescription(description);
    }

    /**
     * Creates the description for a timer according to the setting in the configuration panel.
     *
     * @param descTvh The description provided by tvheadend
     * @param prog    The program, which corresponds to this timer
     * @return the description as String
     */
    public static String createDescription(String descriptionSelectorItemId, String descTvh, Program prog) {
        String descTvb = Optional.ofNullable(prog).map(Program::getDescription).orElse("");
        if (descriptionSelectorItemId.equals(DescriptionSelectorItem.TVH)) {
            return descTvh;
        } else if (descriptionSelectorItemId.equals(DescriptionSelectorItem.TVB_DESC)) {
            return descTvb;
        } else if (descriptionSelectorItemId.equals(DescriptionSelectorItem.LONGEST)) {
            if (descTvh.length() <= descTvb.length()) {
                return descTvb;
            } else {
                return descTvh;
            }
        } else if (descriptionSelectorItemId.startsWith(DescriptionSelectorItem.TVB_PREFIX)) {
            return createFormattedDescription(descriptionSelectorItemId, descTvh, prog);
        } else {
            return descTvh;
        }
    }

    private static String createFormattedDescription(String descriptionSelectorItemId, String descTvh, Program prog) {
        String selectedFormat = descriptionSelectorItemId.substring(descriptionSelectorItemId.indexOf('_') + 1);
        AbstractPluginProgramFormating[] formats = getPluginManager().getAvailableGlobalPuginProgramFormatings();
        for (AbstractPluginProgramFormating format : formats) {
            if (format.getId().equals(selectedFormat)) {
                ParamParser parser = new ParamParser();
                String desc = parser.analyse(format.getContentValue(), prog);
                return desc != null ? desc : descTvh;
            }
        }

        // no format found
        return descTvh;
    }

    /**
     * @return the tvheadend EPG event, this timer has been created for, or 0
     */
    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }
}
