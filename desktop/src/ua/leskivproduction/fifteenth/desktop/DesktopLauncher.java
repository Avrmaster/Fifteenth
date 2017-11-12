package ua.leskivproduction.fifteenth.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import ua.leskivproduction.fifteenth.Fifteenth;

import java.awt.*;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = "Particles Model 3D";
		cfg.useGL30 = true;

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		cfg.width = screenSize.width*2/3;
		cfg.height = screenSize.height*2/3;
//
//		cfg.width = screenSize.width;
//		cfg.height = screenSize.height;
//		cfg.fullscreen=true;

		new LwjglApplication(new Fifteenth(), cfg);
	}
}
