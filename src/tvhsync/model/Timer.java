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
package tvhsync.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

/**
 * Represents a scheduled recording (DVR entry) of tvheadend.
 *
 * Start and end time include the recording buffers (pre and post extra time), like the timers of the original Lazy Bones plugin. The buffers are
 * stored separately, so that the times of the broadcast itself can be sent to tvheadend.
 *
 * @author <a href="hampelratte@users.sf.net">hampelratte@users.sf.net</a>
 */
public class Timer implements Serializable, Comparable<Timer>, Cloneable {

    public static final int INACTIVE = 0;
    public static final int ACTIVE = 1;
    public static final int RECORDING = 8;

    public static final boolean ENABLED = true;

    /** tvheadend priorities */
    public static final int PRIO_IMPORTANT = 0;
    public static final int PRIO_HIGH = 1;
    public static final int PRIO_NORMAL = 2;
    public static final int PRIO_LOW = 3;
    public static final int PRIO_UNIMPORTANT = 4;
    public static final int PRIO_DEFAULT = 6;

    private static final long serialVersionUID = 3L;

    private int state = ACTIVE;

    private Calendar startTime = Calendar.getInstance();

    private Calendar endTime = Calendar.getInstance();

    /** recording buffer before the broadcast in minutes */
    private int preBuffer;

    /** recording buffer after the broadcast in minutes */
    private int postBuffer;

    /** uuid of the tvheadend channel */
    private String channelId = "";

    /** uuid of the DVR entry, empty for timers, which don't exist on the server yet */
    private String id = ""; // NOSONAR

    private int priority = PRIO_DEFAULT;

    /** days, until the recording is removed. 0 means the default of the DVR profile */
    private int lifetime;

    private String title = "";

    private String description = "";

    /** uuid of the DVR profile, empty means the default profile */
    private String dvrConfig = "";

    /** caption of the autorec or timerec rule, which created this entry */
    private String createdBy = "";

    public boolean isActive() {
        return hasState(ACTIVE);
    }

    /**
     * @param state One of INACTIVE, ACTIVE, RECORDING
     * @return true, if the timer has the given state
     */
    public boolean hasState(int state) {
        if (state == INACTIVE) {
            return !isActive();
        } else {
            return (this.state & state) == state;
        }
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
        if (endTime.before(startTime)) {
            endTime.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public int getPreBuffer() {
        return preBuffer;
    }

    public void setPreBuffer(int preBuffer) {
        this.preBuffer = preBuffer;
    }

    public int getPostBuffer() {
        return postBuffer;
    }

    public void setPostBuffer(int postBuffer) {
        this.postBuffer = postBuffer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getDvrConfig() {
        return dvrConfig;
    }

    public void setDvrConfig(String dvrConfig) {
        this.dvrConfig = dvrConfig == null ? "" : dvrConfig;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy == null ? "" : createdBy;
    }

    /**
     * @return A key, which identifies this timer by channel, start and end
     */
    public String getUniqueKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return getChannelId() + ':' + sdf.format(getStartTime().getTime()) + ':' + sdf.format(getEndTime().getTime());
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append(getState()).append(':');
        sb.append(channelId).append(':');
        sb.append(sdf.format(getStartTime().getTime())).append(':');
        sb.append(sdf.format(getEndTime().getTime())).append(':');
        sb.append(priority).append(':');
        sb.append(lifetime).append(':');
        sb.append(title);
        return sb.toString();
    }

    private String identity() {
        return getUniqueKey() + ':' + state + ':' + priority + ':' + lifetime + ':' + preBuffer + ':' + postBuffer + ':' + title + ':'
                + description + ':' + dvrConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Timer) {
            return ((Timer) o).identity().equals(identity());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identity());
    }

    @Override
    public int compareTo(Timer that) {
        return that.identity().compareTo(this.identity());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    @Override
    public Object clone() { // NOSONAR java:S2975
        Timer timer = new Timer();
        copyTo(timer);
        return timer;
    }

    public void copyTo(Timer timer) {
        timer.setID(getID());
        timer.setState(getState());
        timer.setChannelId(getChannelId());
        timer.setDescription(getDescription());
        timer.setStartTime((Calendar) getStartTime().clone());
        timer.setEndTime((Calendar) getEndTime().clone());
        timer.setPreBuffer(getPreBuffer());
        timer.setPostBuffer(getPostBuffer());
        timer.setLifetime(getLifetime());
        timer.setPriority(getPriority());
        timer.setTitle(getTitle());
        timer.setDvrConfig(getDvrConfig());
        timer.setCreatedBy(getCreatedBy());
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id == null ? "" : id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    /**
     * Sets the given state to the given value. All other states are not touched.
     *
     * @param state   the state to change
     * @param enabled the new value of the state
     */
    public void changeStateTo(int state, boolean enabled) {
        if (enabled && hasState(state) || !enabled && !hasState(state)) {
            // we don't have to change anything, because the timer already
            // has the requested state
            return;
        }

        int sign = enabled ? 1 : -1;
        this.state += sign * state;
    }

    public boolean isRecording() {
        if (hasState(ACTIVE) && hasState(RECORDING)) {
            return true;
        }

        Calendar now = Calendar.getInstance();
        if (now.after(getStartTime()) && now.before(getEndTime())) {
            return hasState(ACTIVE);
        }

        return false;
    }
}
