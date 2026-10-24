/*
 * Copyright (c) Henrik Niehaus
 * Copyright (c) 2026 Christian Echterling
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
package tvhsync.gui.settings;

import javax.swing.JComboBox;

import tvhsync.TVHsync;
import tvhsync.model.Timer;

/**
 * Combobox to select one of the priorities of tvheadend
 */
public class PriorityComboBox extends JComboBox<PriorityComboBox.Priority> {

    private static final long serialVersionUID = 1L;

    public PriorityComboBox() {
        addItem(new Priority(Timer.PRIO_DEFAULT, TVHsync.getTranslation("prio_default", "Default")));
        addItem(new Priority(Timer.PRIO_IMPORTANT, TVHsync.getTranslation("prio_important", "Important")));
        addItem(new Priority(Timer.PRIO_HIGH, TVHsync.getTranslation("prio_high", "High")));
        addItem(new Priority(Timer.PRIO_NORMAL, TVHsync.getTranslation("prio_normal", "Normal")));
        addItem(new Priority(Timer.PRIO_LOW, TVHsync.getTranslation("prio_low", "Low")));
        addItem(new Priority(Timer.PRIO_UNIMPORTANT, TVHsync.getTranslation("prio_unimportant", "Unimportant")));
    }

    public void setPriority(int value) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).value == value) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(0);
    }

    public int getPriority() {
        Priority selected = (Priority) getSelectedItem();
        return selected != null ? selected.value : Timer.PRIO_DEFAULT;
    }

    public static String getName(int value) {
        for (Priority p : new PriorityComboBox().items()) {
            if (p.value == value) {
                return p.name;
            }
        }
        return Integer.toString(value);
    }

    private Priority[] items() {
        Priority[] items = new Priority[getItemCount()];
        for (int i = 0; i < items.length; i++) {
            items[i] = getItemAt(i);
        }
        return items;
    }

    public static class Priority {
        private final int value;
        private final String name;

        Priority(int value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
