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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class TitleMapping implements Serializable, TableModel {

    private ArrayList<String> tvhTitles = new ArrayList<>();

    private ArrayList<String> tvbTitles = new ArrayList<>();

    public void put(String tvbTitle, String tvhTitle) {
        if (tvhTitles.contains(tvhTitle)) {
            int index = tvhTitles.indexOf(tvhTitle);
            tvbTitles.set(index, tvbTitle);
        } else {
            tvhTitles.add(tvhTitle);
            tvbTitles.add(tvbTitle);
        }

        fireTableChanged();
    }

    public void removeRow(int index) {
        tvhTitles.remove(index);
        tvbTitles.remove(index);
        fireTableChanged();
    }

    private void fireTableChanged() {
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this));
        }
    }

    public String getTvbTitle(String tvhTitle) {
        int count = 0;
        for (Iterator<String> iter = tvhTitles.iterator(); iter.hasNext(); count++) {
            String element = iter.next();
            if (element.equals(tvhTitle)) {
                return tvbTitles.get(count);
            }
        }
        return null;
    }

    public String getTvhTitle(String tvbTitle) {
        int count = 0;
        for (Iterator<String> iter = tvbTitles.iterator(); iter.hasNext(); count++) {
            String element = iter.next();
            if (element.equals(tvbTitle)) {
                return tvhTitles.get(count);
            }
        }
        return null;
    }

    private transient ArrayList<TableModelListener> listeners = new ArrayList<>();

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "TV-Browser";
        } else {
            return "tvheadend";
        }
    }

    @Override
    public int getRowCount() {
        return tvhTitles.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return tvbTitles.get(rowIndex);
        } else {
            return tvhTitles.get(rowIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            tvbTitles.set(rowIndex, aValue.toString());
        } else {
            tvhTitles.set(rowIndex, aValue.toString());
        }
    }

    public Map<String, String> getAsMap() {
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < tvbTitles.size(); i++) {
            map.put(tvbTitles.get(i), tvhTitles.get(i));
        }
        return map;
    }

    public void setMappingFromMap(Map<String, String> map) {
        for (Iterator<String> iter = map.keySet().iterator(); iter.hasNext();) {
            String key = iter.next();
            String value = map.get(key);
            tvbTitles.add(key);
            tvhTitles.add(value);
        }
    }
}