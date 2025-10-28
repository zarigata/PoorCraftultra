package com.poorcraft.ultra.shared.config;

import java.util.HashMap;
import java.util.Map;

public class Config {

    private Graphics graphics;
    private Controls controls;
    private Audio audio;
    private Network network;
    private Ai ai;
    private Steam steam;
    private Discord discord;

    public Config() {
    }

    public Graphics getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    public Controls getControls() {
        return controls;
    }

    public void setControls(Controls controls) {
        this.controls = controls;
    }

    public Audio getAudio() {
        return audio;
    }

    public void setAudio(Audio audio) {
        this.audio = audio;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public Steam getSteam() {
        return steam;
    }

    public void setSteam(Steam steam) {
        this.steam = steam;
    }

    public Discord getDiscord() {
        return discord;
    }

    public void setDiscord(Discord discord) {
        this.discord = discord;
    }

    public static Config createDefault() {
        Config config = new Config();

        Resolution resolution = new Resolution();
        resolution.setWidth(1280);
        resolution.setHeight(720);

        Graphics graphics = new Graphics();
        graphics.setResolution(resolution);
        graphics.setFullscreen(false);
        graphics.setVsync(true);
        graphics.setMsaa(4);
        graphics.setViewDistance(8);
        graphics.setFov(75);

        Controls controls = new Controls();
        controls.setForward("W");
        controls.setBack("S");
        controls.setLeft("A");
        controls.setRight("D");
        controls.setJump("SPACE");
        controls.setCrouch("LSHIFT");
        controls.setInventory("E");
        controls.setChat("T");
        controls.setCommand("/");

        Audio audio = new Audio();
        audio.setMasterVolume(1.0);
        audio.setMusicVolume(0.7);
        audio.setSfxVolume(0.8);

        Network network = new Network();
        network.setPort(25565);
        network.setMaxPlayers(16);
        network.setTimeout(30);

        Ai ai = new Ai();
        ai.setProvider("OFF");
        ai.setModel("");
        ai.setEndpoints(new HashMap<>());
        ai.setApiKeys(new HashMap<>());

        Steam steam = new Steam();
        steam.setAppId(0L);
        steam.setEnabled(false);

        Discord discord = new Discord();
        discord.setAppId(0L);
        discord.setEnabled(false);

        config.setGraphics(graphics);
        config.setControls(controls);
        config.setAudio(audio);
        config.setNetwork(network);
        config.setAi(ai);
        config.setSteam(steam);
        config.setDiscord(discord);

        return config;
    }

    public static class Graphics {

        private Resolution resolution;
        private boolean fullscreen;
        private boolean vsync;
        private int msaa;
        private int viewDistance;
        private int fov;

        public Graphics() {
        }

        public Resolution getResolution() {
            return resolution;
        }

        public void setResolution(Resolution resolution) {
            this.resolution = resolution;
        }

        public boolean isFullscreen() {
            return fullscreen;
        }

        public void setFullscreen(boolean fullscreen) {
            this.fullscreen = fullscreen;
        }

        public boolean isVsync() {
            return vsync;
        }

        public void setVsync(boolean vsync) {
            this.vsync = vsync;
        }

        public int getMsaa() {
            return msaa;
        }

        public void setMsaa(int msaa) {
            this.msaa = msaa;
        }

        public int getViewDistance() {
            return viewDistance;
        }

        public void setViewDistance(int viewDistance) {
            this.viewDistance = viewDistance;
        }

        public int getFov() {
            return fov;
        }

        public void setFov(int fov) {
            this.fov = fov;
        }
    }

    public static class Resolution {

        private int width;
        private int height;

        public Resolution() {
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }

    public static class Controls {

        private String forward;
        private String back;
        private String left;
        private String right;
        private String jump;
        private String crouch;
        private String inventory;
        private String chat;
        private String command;

        public Controls() {
        }

        public String getForward() {
            return forward;
        }

        public void setForward(String forward) {
            this.forward = forward;
        }

        public String getBack() {
            return back;
        }

        public void setBack(String back) {
            this.back = back;
        }

        public String getLeft() {
            return left;
        }

        public void setLeft(String left) {
            this.left = left;
        }

        public String getRight() {
            return right;
        }

        public void setRight(String right) {
            this.right = right;
        }

        public String getJump() {
            return jump;
        }

        public void setJump(String jump) {
            this.jump = jump;
        }

        public String getCrouch() {
            return crouch;
        }

        public void setCrouch(String crouch) {
            this.crouch = crouch;
        }

        public String getInventory() {
            return inventory;
        }

        public void setInventory(String inventory) {
            this.inventory = inventory;
        }

        public String getChat() {
            return chat;
        }

        public void setChat(String chat) {
            this.chat = chat;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }

    public static class Audio {

        private double masterVolume;
        private double musicVolume;
        private double sfxVolume;

        public Audio() {
        }

        public double getMasterVolume() {
            return masterVolume;
        }

        public void setMasterVolume(double masterVolume) {
            this.masterVolume = masterVolume;
        }

        public double getMusicVolume() {
            return musicVolume;
        }

        public void setMusicVolume(double musicVolume) {
            this.musicVolume = musicVolume;
        }

        public double getSfxVolume() {
            return sfxVolume;
        }

        public void setSfxVolume(double sfxVolume) {
            this.sfxVolume = sfxVolume;
        }
    }

    public static class Network {

        private int port;
        private int maxPlayers;
        private int timeout;

        public Network() {
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public static class Ai {

        private String provider;
        private String model;
        private Map<String, String> endpoints;
        private Map<String, String> apiKeys;

        public Ai() {
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Map<String, String> getEndpoints() {
            if (endpoints == null) {
                endpoints = new HashMap<>();
            }
            return endpoints;
        }

        public void setEndpoints(Map<String, String> endpoints) {
            this.endpoints = endpoints;
        }

        public Map<String, String> getApiKeys() {
            if (apiKeys == null) {
                apiKeys = new HashMap<>();
            }
            return apiKeys;
        }

        public void setApiKeys(Map<String, String> apiKeys) {
            this.apiKeys = apiKeys;
        }
    }

    public static class Steam {

        private long appId;
        private boolean enabled;

        public Steam() {
        }

        public long getAppId() {
            return appId;
        }

        public void setAppId(long appId) {
            this.appId = appId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Discord {

        private long appId;
        private boolean enabled;

        public Discord() {
        }

        public long getAppId() {
            return appId;
        }

        public void setAppId(long appId) {
            this.appId = appId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}
