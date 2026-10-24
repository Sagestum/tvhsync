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
package tvhsync.tvh;

import static tvhsync.tvh.Json.asList;
import static tvhsync.tvh.Json.asMap;
import static tvhsync.tvh.Json.getBoolean;
import static tvhsync.tvh.Json.getInt;
import static tvhsync.tvh.Json.getLong;
import static tvhsync.tvh.Json.getString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tvhsync.model.Channel;
import tvhsync.model.DiskStatus;
import tvhsync.model.EPGEntry;
import tvhsync.model.Recording;
import tvhsync.model.Timer;

/**
 * The calls of the tvheadend HTTP API used by this plugin and the conversion of the JSON responses into model objects.
 */
public class Tvheadend {

    private static final int NO_LIMIT = 100000;

    /** comment, which is stored with every DVR entry created by this plugin */
    private static final String COMMENT = "TVHsync";

    private Tvheadend() {}

    // ############################## server ##############################

    public static Response getServerInfo() {
        return TvhConnection.get("/api/serverinfo", null);
    }

    /**
     * @return The DVR profiles as map uuid -> name. The default profile has an empty name in tvheadend.
     */
    public static Map<String, String> getDvrConfigs(Response res) {
        Map<String, String> configs = new LinkedHashMap<>();
        for (Object o : asList(asMap(res.getBody()).get("entries"))) {
            Map<String, Object> entry = asMap(o);
            if (getBoolean(entry, "enabled")) {
                configs.put(getString(entry, "uuid"), getString(entry, "name"));
            }
        }
        return configs;
    }

    public static Response listDvrConfigs() {
        return TvhConnection.get("/api/dvr/config/grid", params("limit", NO_LIMIT));
    }

    public static Response getDiskStatus() {
        return TvhConnection.get("/comet/poll", null);
    }

    public static DiskStatus parseDiskStatus(Response res) {
        for (Object o : asList(asMap(res.getBody()).get("messages"))) {
            Map<String, Object> msg = asMap(o);
            if (msg.containsKey("totaldiskspace")) {
                long total = getLong(msg, "totaldiskspace");
                long free = getLong(msg, "freediskspace");
                DiskStatus status = new DiskStatus();
                status.setSpaceTotalInBytes(total);
                status.setSpaceFreeInBytes(free);
                status.setUsage(total > 0 ? (int) Math.round((total - free) * 100d / total) : 0);
                return status;
            }
        }
        return null;
    }

    // ############################## channels ##############################

    public static Response listChannels() {
        return TvhConnection.get("/api/channel/grid", params("limit", NO_LIMIT));
    }

    public static List<Channel> parseChannels(Response res) {
        List<Channel> channels = new ArrayList<>();
        for (Object o : asList(asMap(res.getBody()).get("entries"))) {
            Map<String, Object> entry = asMap(o);
            if (entry.containsKey("enabled") && !getBoolean(entry, "enabled")) {
                continue;
            }
            Channel chan = new Channel();
            chan.setId(getString(entry, "uuid"));
            chan.setName(getString(entry, "name"));
            chan.setChannelNumber(getInt(entry, "number"));
            channels.add(chan);
        }
        channels.sort(Comparator.comparingInt(Channel::getChannelNumber).thenComparing(Channel::getName, String.CASE_INSENSITIVE_ORDER));
        return channels;
    }

    // ############################## EPG ##############################

    /**
     * Looks up the EPG events of a channel, which are running at the given time
     *
     * @param channelId uuid of the channel
     * @param timeInMillis the time
     */
    public static Response listEpgEventsAt(String channelId, long timeInMillis) {
        long time = timeInMillis / 1000;
        List<Object> filter = new ArrayList<>();
        filter.add(numericFilter("start", time, "lt"));
        filter.add(numericFilter("stop", time, "gt"));
        return TvhConnection.get("/api/epg/events/grid", params("channel", channelId, "limit", 5, "filter", filter));
    }

    private static Map<String, Object> numericFilter(String field, long value, String comparison) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("field", field);
        filter.put("type", "numeric");
        filter.put("value", value);
        filter.put("comparison", comparison);
        return filter;
    }

    public static List<EPGEntry> parseEpgEvents(Response res) {
        List<EPGEntry> events = new ArrayList<>();
        for (Object o : asList(asMap(res.getBody()).get("entries"))) {
            Map<String, Object> entry = asMap(o);
            EPGEntry epg = new EPGEntry();
            epg.setEventID(getLong(entry, "eventId"));
            epg.setChannelID(getString(entry, "channelUuid"));
            epg.setChannelName(getString(entry, "channelName"));
            epg.setTitle(getString(entry, "title"));
            epg.setShortText(getString(entry, "subtitle"));
            epg.setDescription(getString(entry, "description"));
            epg.setStartTime(getLong(entry, "start") * 1000);
            epg.setEndTime(getLong(entry, "stop") * 1000);
            events.add(epg);
        }
        return events;
    }

    // ############################## timers ##############################

    /**
     * @return all scheduled and currently running recordings
     */
    public static Response listTimers() {
        return TvhConnection.get("/api/dvr/entry/grid_upcoming", params("limit", NO_LIMIT));
    }

    public static List<Timer> parseTimers(Response res) {
        List<Timer> timers = new ArrayList<>();
        for (Object o : asList(asMap(res.getBody()).get("entries"))) {
            timers.add(parseTimer(asMap(o)));
        }
        return timers;
    }

    static Timer parseTimer(Map<String, Object> entry) {
        Timer timer = new Timer();
        timer.setID(getString(entry, "uuid"));
        timer.setChannelId(getString(entry, "channel"));
        timer.setTitle(getString(entry, "disp_title"));
        timer.setDescription(getString(entry, "disp_description"));
        timer.setPriority(getInt(entry, "pri"));
        timer.setLifetime(getInt(entry, "removal"));
        timer.setDvrConfig(getString(entry, "config_name"));
        String createdBy = getString(entry, "autorec_caption");
        if (createdBy.isEmpty()) {
            createdBy = getString(entry, "timerec_caption");
        }
        timer.setCreatedBy(createdBy);

        int state = getBoolean(entry, "enabled") ? Timer.ACTIVE : Timer.INACTIVE;
        if ("recording".equals(getString(entry, "sched_status"))) {
            state |= Timer.RECORDING;
        }
        timer.setState(state);

        // start_extra and stop_extra are 0, if the defaults of the DVR profile are used. In this case
        // we calculate the buffers from the real start and stop times. The real start contains the warm-up time
        // of a few seconds, which gets cut off by the integer division
        long start = getLong(entry, "start");
        long stop = getLong(entry, "stop");
        int pre = getInt(entry, "start_extra");
        if (pre == 0 && entry.containsKey("start_real")) {
            pre = (int) Math.max(0, (start - getLong(entry, "start_real")) / 60);
        }
        int post = getInt(entry, "stop_extra");
        if (post == 0 && entry.containsKey("stop_real")) {
            post = (int) Math.max(0, (getLong(entry, "stop_real") - stop) / 60);
        }
        timer.setPreBuffer(pre);
        timer.setPostBuffer(post);

        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis((start - pre * 60L) * 1000);
        timer.setStartTime(startTime);
        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis((stop + post * 60L) * 1000);
        timer.setEndTime(endTime);
        return timer;
    }

    /**
     * Creates a new DVR entry. If an EPG event ID is given, the entry is linked to the EPG event, so that tvheadend follows changes of the broadcast
     * time.
     *
     * @param timer the timer to create
     * @param eventId the EPG event of tvheadend or 0, if the timer is not based on an EPG event
     */
    public static Response createTimer(Timer timer, long eventId) {
        Map<String, Object> conf = timerToConf(timer);
        if (eventId > 0) {
            conf.put("broadcast", eventId);
        }
        conf.put("comment", COMMENT);
        return TvhConnection.post("/api/dvr/entry/create", params("conf", conf));
    }

    /**
     * Saves the changed settings of an existing DVR entry
     */
    public static Response saveTimer(Timer timer) {
        Map<String, Object> node = timerToConf(timer);
        node.put("uuid", timer.getID());
        if (timer.isRecording()) {
            // the start time of running recordings can't be changed
            node.remove("start");
            node.remove("start_extra");
            node.remove("channel");
        }
        return TvhConnection.post("/api/idnode/save", params("node", node));
    }

    private static Map<String, Object> timerToConf(Timer timer) {
        long start = timer.getStartTime().getTimeInMillis() / 1000 + timer.getPreBuffer() * 60L;
        long stop = timer.getEndTime().getTimeInMillis() / 1000 - timer.getPostBuffer() * 60L;
        Map<String, Object> conf = new LinkedHashMap<>();
        conf.put("enabled", timer.isActive());
        conf.put("channel", timer.getChannelId());
        conf.put("start", start);
        conf.put("stop", stop);
        conf.put("start_extra", timer.getPreBuffer());
        conf.put("stop_extra", timer.getPostBuffer());
        conf.put("disp_title", timer.getTitle());
        if (!timer.getDescription().isEmpty()) {
            conf.put("disp_description", timer.getDescription());
        }
        if (!timer.getDvrConfig().isEmpty()) {
            conf.put("config_name", timer.getDvrConfig());
        }
        conf.put("pri", timer.getPriority());
        conf.put("removal", timer.getLifetime());
        return conf;
    }

    /**
     * Deletes a scheduled DVR entry
     */
    public static Response deleteTimer(Timer timer) {
        return TvhConnection.post("/api/idnode/delete", params("uuid", timer.getID()));
    }

    /**
     * Stops a running recording. The part, which has been recorded so far, is kept.
     */
    public static Response stopRecording(String uuid) {
        return TvhConnection.post("/api/dvr/entry/stop", params("uuid", uuid));
    }

    // ############################## recordings ##############################

    public static Response listRecordings() {
        return TvhConnection.get("/api/dvr/entry/grid_finished", params("limit", NO_LIMIT));
    }

    public static List<Recording> parseRecordings(Response res) {
        List<Recording> recordings = new ArrayList<>();
        for (Object o : asList(asMap(res.getBody()).get("entries"))) {
            Map<String, Object> entry = asMap(o);
            Recording rec = new Recording();
            rec.setId(getString(entry, "uuid"));
            String title = getString(entry, "disp_title");
            String directory = getString(entry, "directory").replace('/', '~');
            while (directory.startsWith("~")) {
                directory = directory.substring(1);
            }
            while (directory.endsWith("~")) {
                directory = directory.substring(0, directory.length() - 1);
            }
            rec.setTitle(directory.isEmpty() ? title : directory + '~' + title);
            rec.setShortText(getString(entry, "disp_subtitle"));
            rec.setDescription(getString(entry, "disp_description"));
            rec.setChannelID(getString(entry, "channel"));
            rec.setChannelName(getString(entry, "channelname"));
            rec.setEventID(getLong(entry, "broadcast"));
            // start and end of the broadcast, the duration is the real length of the recording in minutes
            long start = getLong(entry, "start");
            long stop = getLong(entry, "stop");
            rec.setStartTime(start * 1000);
            rec.setEndTime(stop * 1000);
            long realStart = entry.containsKey("start_real") ? getLong(entry, "start_real") : start;
            long realStop = entry.containsKey("stop_real") ? getLong(entry, "stop_real") : stop;
            long duration = getLong(entry, "duration") > 0 ? getLong(entry, "duration") : realStop - realStart;
            rec.setDuration((int) Math.max(0, duration / 60));
            rec.setNew(getLong(entry, "playcount") == 0);
            rec.setHasError(getInt(entry, "errorcode") != 0 || getLong(entry, "errors") > 0);
            rec.setPriority(getInt(entry, "pri"));
            rec.setLifetime(getInt(entry, "removal"));
            rec.setFileSize(getLong(entry, "filesize"));
            recordings.add(rec);
        }
        Collections.sort(recordings);
        return recordings;
    }

    /**
     * Deletes a finished recording including the file
     */
    public static Response deleteRecording(Recording rec) {
        return TvhConnection.post("/api/dvr/entry/remove", params("uuid", rec.getId()));
    }

    /**
     * Changes the title of a recording. The file on the server is not renamed.
     */
    public static Response renameRecording(Recording rec, String newTitle) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("uuid", rec.getId());
        node.put("disp_title", newTitle);
        return TvhConnection.post("/api/idnode/save", params("node", node));
    }

    // ############################## stream URLs ##############################

    public static String getChannelStreamUrl(Channel chan) {
        return TvhConnection.getBaseUrlWithCredentials() + "/stream/channel/" + chan.getId();
    }

    public static String getRecordingStreamUrl(Recording rec) {
        return TvhConnection.getBaseUrlWithCredentials() + "/dvrfile/" + rec.getId();
    }

    private static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            params.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return params;
    }
}
