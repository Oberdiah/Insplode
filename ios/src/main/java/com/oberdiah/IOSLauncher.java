package com.oberdiah;

import org.jetbrains.annotations.NotNull;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.oberdiah.utils.PlatformInterface;

import games.rednblack.miniaudio.MiniAudio;

class IOSPlatformInterface implements PlatformInterface {
    @Override
    public void print(@NotNull String string) {
        System.out.println("Foo!" + string);
    }

    @Override
    public void injectAssetManager(@NotNull MiniAudio miniAudio) {
        miniAudio.setupAndroid(null);
    }
}

/**
 * Launches the iOS (RoboVM) application.
 */
public class IOSLauncher extends IOSApplication.Delegate {
    IOSPlatformInterface platformInterface;

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
        configuration.hideHomeIndicator = true;
        configuration.useHaptics = true;
        configuration.hdpiMode = HdpiMode.Pixels;
        configuration.useCompass = false;
        platformInterface = new IOSPlatformInterface();
        return new IOSApplication(new Main(platformInterface), configuration);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}