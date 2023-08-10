package winter.slayerkph;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import java.awt.image.BufferedImage;


class SlayerKphCounter extends Counter {
	SlayerKphCounter(BufferedImage img, Plugin plugin, int amount) {
		super(img, plugin, amount);
	}
}
