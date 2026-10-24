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

import java.io.InputStream;

import tvhsync.model.Channel;
import tvhsync.model.Recording;
import tvhsync.tvh.TvhConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import devplugin.Program;
import util.i18n.Localizer;

/**
 * Starts a player to watch a channel or a recording via the HTTP streaming of tvheadend
 *
 * @author <a href="hampelratte@users.sf.net">hampelratte@users.sf.net </a>
 *
 */
public class Player {
    private static PlayerThread playerThread;

    private static Logger logger = LoggerFactory.getLogger(Player.class);

    private Player() {}

    /**
     * @return true, if an external player has been configured in the settings
     */
    public static boolean isConfigured() {
        String player = TVHsync.getProperties().getProperty("player");
        return player != null && !player.trim().isEmpty();
    }

    public static void play(Program prog) {
        Channel chan = ChannelManager.getChannelMapping().get(prog.getChannel().getId());
        if (chan != null) {
            Player.play(chan);
        } else {
            logger.error("Couldn't start Player. No mapped channel found");
        }
    }

    public static void play(Channel channel) {
        try {
            String url = TVHsync.getProperties().getProperty("streamurl");
            url = replaceBaseUrl(url);
            url = url.replace("<channel>", channel.getId());
            start(url);
        } catch (Exception e1) {
            String mesg = Localizer.getLocalization(Localizer.I18N_ERROR) + ": " + e1;
            logger.error(mesg);
        }
    }

    public static void play(Recording rec) {
        String url = TVHsync.getProperties().getProperty("recording.url");
        url = replaceBaseUrl(url);
        url = url.replace("<recording>", rec.getId());
        start(url);
    }

    private static String replaceBaseUrl(String url) {
        return url.replace("<url>", TvhConnection.getBaseUrlWithCredentials());
    }

    private static void start(String url) {
        if (playerThread != null && playerThread.isRunning()) {
            playerThread.stopThread();
        }

        String parameters = TVHsync.getProperties().getProperty("player_params");
        String[] arguments;
        if (parameters != null && parameters.trim().length() > 0) {
            String[] params = parameters.trim().split(" ");
            arguments = new String[params.length + 2];
            System.arraycopy(params, 0, arguments, 1, params.length);
        } else {
            arguments = new String[2];
        }
        arguments[0] = TVHsync.getProperties().getProperty("player");
        // don't log the credentials
        logger.debug("Trying to play url {}", url.replace(TvhConnection.getBaseUrlWithCredentials(), TvhConnection.getBaseUrl()));
        arguments[arguments.length - 1] = url;
        playerThread = new PlayerThread(arguments);
    }

    public static void stop() {
        if (playerThread != null) {
            playerThread.stopThread();
        }
    }

    private static class PlayerThread extends Thread {
        private boolean running = false;

        private String[] playerParams;

        private Process p;

        PlayerThread(String[] playerParams) {
            this.playerParams = playerParams;
            setName(getClass().getName());
            start();
        }

        @Override
        public void run() {
            running = true;
            Runtime rt = Runtime.getRuntime();
            try {
                p = rt.exec(playerParams);
                new PlayerOutputter(p.getInputStream());
                new PlayerOutputter(p.getErrorStream());
                p.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String mesg = TVHsync.getTranslation("couldnt_start", "Couldn't start player");
                logger.error(mesg, e);
            } catch (Exception e) {
                String mesg = TVHsync.getTranslation("couldnt_start", "Couldn't start player");
                logger.error(mesg, e);
            }
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        public void stopThread() {
            if (p != null) {
                p.destroy();
            }
        }
    }

    private static class PlayerOutputter extends Thread {
        private InputStream in;

        PlayerOutputter(InputStream in) {
            this.in = in;
            setName(getClass().getName());
            start();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int length = -1;
            try {
                while ((length = in.read(buffer)) > 0) {
                    logger.debug("PLAYER: {}", new String(buffer, 0, length));
                }
            } catch (Exception e) {
                // not interested in player output or any errors
            }
        }
    }
}