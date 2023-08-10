package winter.slayerkph;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("slayerkph")
public interface SlayerKphConfig extends Config
{
	@ConfigItem(
		keyName = "samplesize",
		name = "Sample Size",
		description = "Determines the amount of monsters to consider for calcluating the hourly rate"
	)
	default int sampleSize()
	{
		return 10;
	}
}
