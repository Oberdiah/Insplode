package com.oberdiah.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.oberdiah.Main;
import com.oberdiah.utils.PlatformInterface;

import org.jetbrains.annotations.NotNull;

import games.rednblack.miniaudio.MiniAudio;

class Lwjgl3Implementer implements PlatformInterface {
    @Override
    public void print(@NotNull String string) {
        System.out.println(string);
    }

    @Override
    public void injectAssetManager(@NotNull MiniAudio miniAudio) {
        // Do nothing
    }
}

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(new Lwjgl3Implementer()), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("WeeklyGame2");
        configuration.setWindowedMode(450, 960);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}