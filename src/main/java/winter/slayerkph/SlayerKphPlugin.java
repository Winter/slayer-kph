package winter.slayerkph;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.ItemID;
import net.runelite.api.Varbits;
import net.runelite.api.VarPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(name = "Slayer Kph")
public class SlayerKphPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SlayerKphConfig config;

	private int prevTaskSize;
	private SlayerKphCounter counter;
	private List<Instant> killTimeSamples = new LinkedList<>();

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged) {
		int varpId = varbitChanged.getVarpId();
		int varbitId = varbitChanged.getVarbitId();

		if (varpId == VarPlayer.SLAYER_TASK_SIZE
			|| varpId == VarPlayer.SLAYER_TASK_CREATURE
			|| varpId == VarPlayer.SLAYER_TASK_LOCATION
			|| varbitId == Varbits.SLAYER_TASK_BOSS)
		{
			int taskId = client.getVarpValue(VarPlayer.SLAYER_TASK_CREATURE);
			int taskSize = client.getVarpValue(VarPlayer.SLAYER_TASK_SIZE);
			String taskName = client.getEnum(EnumID.SLAYER_TASK_CREATURE)
					.getStringValue(taskId);

			clientThread.invoke(() -> onTaskUpdated(taskSize, taskName));
		}
	}

	@Provides
	SlayerKphConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SlayerKphConfig.class);
	}

	private void onTaskUpdated(int taskSize, String taskName) {
		// Task had been completed.
		if (taskSize <= 0) {
			removeKphCounter();
			log.debug("Task size was set to zero");
			return;
		}

		// Task amount remaining has changed.
		if (taskSize != this.prevTaskSize) {
			//It's possible that the taskSize could change by more than one in a single tick.
			int taskChangedBy = this.prevTaskSize - taskSize;

			for (int i = 0; i < taskChangedBy; i++) {
				addKillSample();
			}

			if (killTimeSamples.size() >= config.sampleSize()) {
				if (counter != null) {
					counter.setCount(calculateKph());
				} else {
					addKphCounter(taskName);
				}
			}
		}

		//Save our current task size
		this.prevTaskSize = taskSize;
	}

	private void addKillSample() {
		killTimeSamples.add(Instant.now());

		log.debug("Added kill time sample");

		if (killTimeSamples.size() > config.sampleSize()) {
			killTimeSamples.remove(0);
		}
	}
	
	private int calculateKph() {
		double elapsedTimeMillis = Duration
				.between(killTimeSamples.get(0), killTimeSamples.get(killTimeSamples.size() - 1))
				.toMillis();

		return (int) (killTimeSamples.size() / elapsedTimeMillis * TimeUnit.HOURS.toMillis(1));
	}
	
	// Mostly just copied and pasted from the slayer plugin
	private void addKphCounter(String taskName) {
		if (counter != null || Strings.isNullOrEmpty(taskName)) {
			return;
		}
		
		Task task = Task.getTask(taskName);
		int itemSpriteId = ItemID.ENCHANTED_GEM;
		if (task != null) {
			itemSpriteId = task.getItemSpriteId();
		}

		BufferedImage taskImg = itemManager.getImage(itemSpriteId);
		
		counter = new SlayerKphCounter(taskImg, this, calculateKph());
		counter.setTooltip("Kills per hour on your current slayer task");
		infoBoxManager.addInfoBox(counter);

		log.debug("Added counter");
	}
	
	private void removeKphCounter() {
		if (counter == null) {
			return;
		}

		infoBoxManager.removeInfoBox(counter);
		killTimeSamples.clear();
		counter = null;

		log.debug("Removed counter");
	}
}
