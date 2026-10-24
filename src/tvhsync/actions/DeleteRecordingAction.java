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
package tvhsync.actions;

import javax.swing.JOptionPane;

import tvhsync.tvh.Response;
import tvhsync.model.Recording;

import tvhsync.TVHsync;
import tvhsync.RecordingManager;
import tvhsync.TvhCallback;
import tvhsync.tvh.Tvheadend;

public class DeleteRecordingAction extends TvhAction {

    private RecordingManager recordingManager;
    private final Recording recording;

    public DeleteRecordingAction(RecordingManager recordingManager, Recording recording) {
        this(recordingManager, recording, null);
    }

    public DeleteRecordingAction(RecordingManager recordingManager, Recording recording, TvhCallback<?> callback) {
        super(callback);
        this.recordingManager = recordingManager;
        this.recording = recording;
    }

    @Override
    boolean execute() {
        int result = JOptionPane.showConfirmDialog(TVHsync.getInstance().getMainDialog(),
                TVHsync.getTranslation("recording_delete", "Do you really want to delete the recording {0}?", recording.getDisplayTitle()), "",
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return true;
        }

        response = Tvheadend.deleteRecording(recording);
        if (!response.isSuccess()) {
            return false;
        }

        // update recording list
        recordingManager.synchronize();
        return true;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public String getDescription() {
        return "Delete recording " + recording;
    }
}