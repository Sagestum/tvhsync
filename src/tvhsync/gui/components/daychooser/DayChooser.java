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
package tvhsync.gui.components.daychooser;

import java.text.DateFormat;
import java.util.Calendar;

import net.sf.nachocalendar.CalendarFactory;
import net.sf.nachocalendar.components.DatePanel;
import net.sf.nachocalendar.event.DateSelectionEvent;
import net.sf.nachocalendar.event.DateSelectionListener;
import net.sf.nachocalendar.model.DateSelectionModel;
import tvhsync.TvhTimer;
import tvhsync.gui.components.timeroptions.TimerOptionsEditor;

/**
 * Lets the user choose the day of a timer. tvheadend doesn't support repeating DVR entries, so only a single date can be chosen.
 */
public class DayChooser extends BrowsePanel implements DateSelectionListener {

    private static final long serialVersionUID = -2936338063641916673L;

    private TvhTimer timer;
    private TimerOptionsEditor timerOptionsEditor;

    private final DatePanel cal = CalendarFactory.createDatePanel();

    public DayChooser() {
        initGUI();
    }

    public void setTimer(TvhTimer timer, TimerOptionsEditor timerOptionsEditor) {
        this.timer = timer;
        this.timerOptionsEditor = timerOptionsEditor;
    }

    private void initGUI() {
        add(cal);
        cal.setSelectionMode(DateSelectionModel.SINGLE_SELECTION);
        cal.getDateSelectionModel().addDateSelectionListener(this);
        this.setSize(300, 200);
    }

    @Override
    public void valueChanged(DateSelectionEvent e) {
        Object o = cal.getDateSelectionModel().getSelectedDate();
        if (o != null) {
            Calendar startDate = Calendar.getInstance();
            startDate.setTimeInMillis(cal.getDate().getTime());

            Calendar start = timer.getStartTime();
            Calendar end = timer.getEndTime();

            long startEndDiff = end.getTimeInMillis() - start.getTimeInMillis();

            start.set(Calendar.YEAR, startDate.get(Calendar.YEAR));
            start.set(Calendar.MONTH, startDate.get(Calendar.MONTH));
            start.set(Calendar.DAY_OF_MONTH, startDate.get(Calendar.DAY_OF_MONTH));

            end.setTimeInMillis(start.getTimeInMillis() + startEndDiff);

            userInputHappened();
        }
    }

    private void userInputHappened() {
        super.textfield.setText(getDayString(timer));
        timerOptionsEditor.startTimeChanged();
    }

    public static String getDayString(TvhTimer timer) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(timer.getStartTime().getTime());
    }
}
