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
package tvhsync.actions;

import java.util.List;

import javax.swing.JOptionPane;

import tvhsync.TVHsync;
import tvhsync.TvhTimer;
import tvhsync.TvhCallback;
import tvhsync.actions.responses.TimersOutOfSync;
import tvhsync.model.Timer;
import tvhsync.tvh.Response;
import tvhsync.tvh.Tvheadend;

public class DeleteTimerAction extends TvhAction {

    private final TvhTimer timer;
    private boolean forceDeletion = false;

    public DeleteTimerAction(TvhTimer timer) {
        this(timer, null);
    }

    public DeleteTimerAction(TvhTimer timer, TvhCallback<DeleteTimerAction> callback) {
        setCallback(callback);
        this.timer = timer;
    }

    /**
     * 
     * @param timer
     *            The timer to delete.
     * @param forceDeletion
     *            If set, the timer will be deleted without user confirmation
     */
    public DeleteTimerAction(TvhTimer timer, boolean forceDeletion) {
        this(timer);
        this.forceDeletion = forceDeletion;
    }

    @Override
    boolean execute() {
        // check, if the timer still exists on the server
        response = Tvheadend.listTimers();
        if (!response.isSuccess()) {
            return false;
        }
        Timer serverTimer = findTimer(Tvheadend.parseTimers(response), timer.getID());
        if (serverTimer == null) {
            response = new TimersOutOfSync();
            return false;
        }

        // check if timer is recording
        if (serverTimer.hasState(Timer.RECORDING)) {
            if (!forceDeletion) {
                int result = JOptionPane.showConfirmDialog(TVHsync.getInstance().getParent(),
                        TVHsync.getTranslation("recording_timer_delete", "This timer is currently recording! Do you really want to delete it?"), "",
                        JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.OK_OPTION) {
                    // do nothing
                    return true;
                }
            }
            // stop the recording, the part recorded so far is kept as finished recording
            response = Tvheadend.stopRecording(timer.getID());
        } else {
            response = Tvheadend.deleteTimer(timer);
        }
        return response.isSuccess();
    }

    static Timer findTimer(List<Timer> timers, String id) {
        for (Timer t : timers) {
            if (t.getID().equals(id)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public String getDescription() {
        return "Delete timer " + timer;
    }
}