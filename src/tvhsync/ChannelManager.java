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

import static devplugin.Plugin.getPluginManager;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tvhsync.model.Channel;
import tvhsync.tvh.Response;
import tvhsync.tvh.Tvheadend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManager {

    private static Logger logger = LoggerFactory.getLogger(ChannelManager.class);

    private static ChannelManager instance;

    private static Map<String, Channel> channelMapping = new Hashtable<>(); // NOSONAR this gets serialized -> can't change the type

    private List<Channel> channels = null;

    /**
     * Loads the channel list from tvheadend
     *
     * @return the response of tvheadend
     */
    public Response update() {
        Response res = Tvheadend.listChannels();
        if (res.isSuccess()) {
            channels = Tvheadend.parseChannels(res);
            logger.debug("{} channels loaded from tvheadend", channels.size());
        } else {
            logger.warn("Couldn't update channel list: {}", res.getMessage());
        }
        return res;
    }

    public static ChannelManager getInstance() {
        if (instance == null) {
            instance = new ChannelManager();
        }
        return instance;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public Channel getChannelById(String channelId) {
        if (channels == null) {
            return null;
        }

        for (Iterator<Channel> iter = channels.iterator(); iter.hasNext();) {
            Channel chan = iter.next();
            if (chan.getId().equals(channelId)) {
                return chan;
            }
        }

        return null;
    }

    /**
     * @return the number of the tvheadend channel of the timer or Integer.MAX_VALUE, if the channel is unknown
     */
    public int getChannelNumber(TvhTimer timer) {
        Channel chan = getChannelById(timer.getChannelId());
        return chan != null ? chan.getChannelNumber() : Integer.MAX_VALUE;
    }

    public static Map<String, Channel> getChannelMapping() {
        return channelMapping;
    }

    public static void setChannelMapping(Map<String, Channel> channelMapping) {
        ChannelManager.channelMapping = channelMapping;
    }

    public devplugin.Channel getTvbrowserChannel(TvhTimer timer) throws ChannelNotFoundException {
        devplugin.Channel chan = null;
        for (String channelID : channelMapping.keySet()) {
            Channel channel = ChannelManager.getChannelMapping().get(channelID);
            if (channel.getId().equals(timer.getChannelId())) {
                chan = getTvbrowserChannelById(channelID);
            }
        }

        if (chan == null) {
            throw new ChannelNotFoundException();
        } else {
            return chan;
        }
    }

    public devplugin.Channel getTvbrowserChannel(Channel chan) throws ChannelNotFoundException {
        devplugin.Channel tvbchan = null;
        for (Entry<String, Channel> entry : channelMapping.entrySet()) {
            Channel ctemp = entry.getValue();
            if (ctemp.getId().equals(chan.getId())) {
                tvbchan = getTvbrowserChannelById(entry.getKey());
            }
        }

        if (tvbchan == null) {
            throw new ChannelNotFoundException();
        } else {
            return tvbchan;
        }
    }

    public devplugin.Channel getTvbrowserChannelById(String id) {
        devplugin.Channel[] subscribedChannels = getPluginManager().getSubscribedChannels();
        for (int i = 0; i < subscribedChannels.length; i++) {
            if (subscribedChannels[i].getId().equals(id)) {
                return subscribedChannels[i];
            }
        }
        return null;
    }

    public static class ChannelNotFoundException extends Exception {

    }
}