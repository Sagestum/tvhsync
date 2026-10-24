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
package tvhsync.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;

import tvhsync.TVHsync;
import tvhsync.RecordingManager;
import tvhsync.TimerManager;
import tvhsync.gui.recordings.RecordingManagerPanel;
import tvhsync.gui.timers.TimerManagerPanel;
import util.ui.UiUtilities;
import util.ui.WindowClosingIf;

public class MainDialog extends JDialog implements WindowClosingIf {

    private JTabbedPane tabbedPane = new JTabbedPane();

    private TimelinePanel timelinePanel;

    private static final int INDEX_TIMELINE = 0;

    public MainDialog(Frame parent, TimerManager timerManager, RecordingManager recordingManager) {
        super(parent, TVHsync.getTranslation("tvhsync", "TVHsync"), false);
        initGUI(timerManager, recordingManager);
        UiUtilities.registerForClosing(this);
    }

    private void initGUI(TimerManager timerManager, RecordingManager recordingManager) {
        timelinePanel = new TimelinePanel(timerManager);

        // !! don't change the order without changing INDEX_TIMELINE !!
        tabbedPane.add(TVHsync.getTranslation("timeline", "Timeline"), timelinePanel);
        tabbedPane.add(TVHsync.getTranslation("timers", "Timers"), new TimerManagerPanel(timerManager, recordingManager));
        tabbedPane.add(TVHsync.getTranslation("recordings", "Recordings"), new RecordingManagerPanel(recordingManager));

        this.add(tabbedPane);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                setVisible(false);
            }
        });

        setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        this.pack();

        // position the dialog centered on tvb
        int parentWidth = TVHsync.getInstance().getParent().getWidth();
        int parentHeight = TVHsync.getInstance().getParent().getHeight();
        int parentX = TVHsync.getInstance().getParent().getX();
        int parentY = TVHsync.getInstance().getParent().getY();
        int posX = (parentWidth - getWidth()) / 2;
        int posY = (parentHeight - getHeight()) / 2;
        setLocation(parentX + posX, parentY + posY);
    }

    public void showTimeline() {
        tabbedPane.setSelectedIndex(INDEX_TIMELINE);
    }

    public TimelinePanel getTimelinePanel() {
        return timelinePanel;
    }

    @Override
    public void close() {
        setVisible(false);
    }
}
