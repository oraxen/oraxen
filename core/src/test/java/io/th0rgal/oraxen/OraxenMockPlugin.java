package io.th0rgal.oraxen;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.willfp.ecoitems.EcoItemsPlugin;
import io.lumine.mythiccrucible.MythicCrucible;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class OraxenMockPlugin {

    public ServerMock server;
    public World world;
    public OraxenPlugin plugin;
    public MythicCrucible crucible;
    public MMOItems mmoItems;
    public EcoItemsPlugin ecoItems;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(OraxenPlugin.class);
        crucible = MockBukkit.load(MythicCrucible.class);
        mmoItems = MockBukkit.load(MMOItems.class);
        ecoItems = MockBukkit.load(EcoItemsPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }
}
