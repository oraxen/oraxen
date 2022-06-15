package io.th0rgal.oraxen.api;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.inventory.ItemStack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;


import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicListener.getBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;


public class CustomBlock {
	
	private String id;
	private TypeBlock type;
	private @Nullable Location loc;
	
	public enum TypeBlock{
		NOTE_BLOCK,
		MUSHROOM,
		WIRE
	}

	public CustomBlock(String id, TypeBlock type) {
		this.id = id;
		this.type = type;
	}
	public CustomBlock(String id, TypeBlock type,@Nullable Location loc) {
		this.id = id;
		this.type = type;
		this.loc = loc;
	}

	
    /**
     * Gets a CustomBlock instance through the provided id.
     * <br>This may return null if the provided id are invalid.
     *
     * @param ID id in the format {@code id}
     * @return Possibly-null CustomBlock instance.
     */
    @Nullable
    public static CustomBlock getInstance(String ID)
    {
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(ID) != null) {
    		return new CustomBlock(ID, TypeBlock.NOTE_BLOCK);
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(ID) != null) {
    		return new CustomBlock(ID, TypeBlock.MUSHROOM);
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(ID) != null) {
    		return new CustomBlock(ID, TypeBlock.WIRE);
    	}
    	return null;
    }

    /**
     * Gets a CustomBlock instance through the provided Bukkit ItemStack.
     * <br>This may return null if the provided Bukkit ItemStack is not associated with any CustomBlock.
     *
     * @param itemStack The Bukkit ItemStack to get the CustomBlock instance from.
     * @return Possibly-null CustomBlock instance.
     */
    @Nullable
    public static CustomBlock byItemStack(ItemStack itemStack)
    {
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(OraxenItems.getIdByItem(itemStack)) != null) {
    		return new CustomBlock(OraxenItems.getIdByItem(itemStack), TypeBlock.NOTE_BLOCK);
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(OraxenItems.getIdByItem(itemStack)) != null) {
    		return new CustomBlock(OraxenItems.getIdByItem(itemStack), TypeBlock.MUSHROOM);
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(OraxenItems.getIdByItem(itemStack)) != null) {
    		return new CustomBlock(OraxenItems.getIdByItem(itemStack), TypeBlock.WIRE);
    	}
    	return null;
    }
    

    /**
     * Places a CustomBlock provided through the ID at the provided location and returns the CustomBlock
     * instance for it.
     * <br>This may return null if the provided ID are invalid.
     *
     * @param ID ID in the format {@code id}
     * @param location     The location to place the CustomBlock at.
     * @return Possibly-null CustomBlock instance.
     */
    @Nullable
    public static CustomBlock place(String ID, Location location)
    {
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(ID) != null) {
    		NoteBlockMechanicFactory.setBlockModel(location.getBlock(), ID);
    		return new CustomBlock(ID, TypeBlock.NOTE_BLOCK,location);
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(ID) != null) {
    		BlockMechanicFactory.setBlockModel(location.getBlock(), ID);
    		return new CustomBlock(ID, TypeBlock.MUSHROOM,location);
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(ID) != null) {
    		StringBlockMechanicFactory.setBlockModel(location.getBlock(), ID);
    		return new CustomBlock(ID, TypeBlock.WIRE,location);
    	}
		return null;
    }
    

    /**
     * Places a CustomBlock through the provided location and returns a CustomBlock instance for it.
     *
     * @param location The location to place the CustomBlock at.
     * @return CustomBlock instance from the location.
     */
    public CustomBlock place(Location location)
    {
    	return CustomBlock.place(id, location);
    }

    /**
     * Gets a CustomBlock instance through the provided Bukkit Block.
     * <br>This may return null if the provided Block is not associated with a CustomBlock.
     *
     * @param block The Bukkit Block to get the CustomBlock from.
     * @return Possibly-null CustomBlock instance.
     */
    @Nullable
    public static CustomBlock byAlreadyPlaced(Block block)
    {
    	if(getNoteBlockMechanic(block) != null) {
    		return new CustomBlock(getNoteBlockMechanic(block).getItemID(), TypeBlock.NOTE_BLOCK);
    	}
    	else if(getBlockMechanic(block) != null) {
    		return new CustomBlock(getBlockMechanic(block).getItemID(), TypeBlock.MUSHROOM);
    	}
    	else if(getStringMechanic(block) != null) {
    		return new CustomBlock(getStringMechanic(block).getItemID(), TypeBlock.WIRE);
    	}
		return null;
    }

    /**
     * Returns whether removing this CustomBlock was successful or not.
     * <br>A removal is successful if the Block associated with this CustomBlock exists.
     *
     * @return true if the CustomBlock could be removed, otherwise false.
     */
    public boolean remove()
    {
    	Block block = loc.getBlock();
    	
    	if(getNoteBlockMechanic(block) != null) {
    		block.setType(Material.AIR);
    		return true;
    	}
    	else if(getBlockMechanic(block) != null) {
    		block.setType(Material.AIR);
    		return true;
    	}
    	else if(getStringMechanic(block) != null) {
    		block.setType(Material.AIR);
    		return true;
    	}
		return false;
    }

    /**
     * Play the block break particles and sound for this custom block.
     * @return true if the effect was played successfully because the bukkitBlock is a custom block, otherwise false.
     */
    public boolean playBreakEffect()
    {
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc.add(0.5,0.5,0.5), 1, 1, 0.1, 0.1, 0.1,this.getBaseBlockData());
    		return true;
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc.add(0.5,0.5,0.5), 1, 1, 0.1, 0.1, 0.1,this.getBaseBlockData());
    		return true;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc.add(0.5,0.5,0.5), 1, 1, 0.1, 0.1, 0.1,this.getBaseBlockData());
    		return true;
    	}
		return false;
    }

    /**
     * Play the block break particles and sound for this custom block.
     * @param bukkitBlock the location of which block you want to play break effect of.
     * @return true if the effect was played successfully because the bukkitBlock is a custom block, otherwise false.
     */
    public static boolean playBreakEffect(Block bukkitBlock)
    {
    	if(getNoteBlockMechanic(bukkitBlock) != null) {
    		bukkitBlock.getLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, bukkitBlock.getLocation().add(0.5,0.5,0.5), 1, 1, 0.1, 0.1, 0.1,bukkitBlock.getBlockData());
    		return true;
    	}
    	else if(getBlockMechanic(bukkitBlock) != null) {
    		bukkitBlock.getLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, bukkitBlock.getLocation().add(0.5,0.5,0.5), 1, 1, 0.1, 0.1, 0.1,bukkitBlock.getBlockData());
    		return true;
    	}
    	else if(getStringMechanic(bukkitBlock) != null) {
    		bukkitBlock.getLocation().getWorld().spawnParticle(Particle.BLOCK_CRACK, bukkitBlock.getLocation().add(0.5,0.5,0.5), 1, 1, 0.1, 0.1, 0.1,bukkitBlock.getBlockData());
    		return true;
    	}
		return false;
    }

    /**
     * Play the block break sound for this custom block.
     * @return true if the sound was played successfully because the bukkitBlock is a custom block, otherwise false.
     */
    public boolean playBreakSound()
    {
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		Sound s = Sound.valueOf(NoteBlockMechanicFactory.getInstance().getMechanic(id).getSection().getString("break_sound"));
    		loc.getWorld().playSound(loc, s, 1.0f, 0.8f);
    		return true;
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		Sound s = Sound.valueOf(BlockMechanicFactory.getInstance().getMechanic(id).getSection().getString("break_sound"));
    		loc.getWorld().playSound(loc, s, 1.0f, 0.8f);
    		return true;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		Sound s = Sound.valueOf(StringBlockMechanicFactory.getInstance().getMechanic(id).getSection().getString("break_sound"));
    		loc.getWorld().playSound(loc, s, 1.0f, 0.8f);
    		return true;
    	}
		return false;
    }

    /**
     * Play the block break sound for this custom block.
     * @param bukkitBlock the location of which block you want to play break sound of.
     * @return true if the effect was played successfully because the bukkitBlock is a custom block, otherwise false.
     */
    public static boolean playBreakSound(Block bukkitBlock)
    {
    	return CustomBlock.byAlreadyPlaced(bukkitBlock).playBreakSound();
    }
    /**
     * Play the block place particles for this custom block.
     * @return true if the effect was played successfully because the bukkitBlock is a custom block, otherwise false.
     */
    public boolean playPlaceSound()
    {
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		Sound s = Sound.valueOf(((NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id)).getPlaceSound());
    		loc.getWorld().playSound(loc, s, 1.0f, 0.8f);
    		return true;
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		Sound s = Sound.valueOf(((BlockMechanic) BlockMechanicFactory.getInstance().getMechanic(id)).getPlaceSound());
    		loc.getWorld().playSound(loc, s, 1.0f, 0.8f);
    		return true;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		Sound s = Sound.valueOf(((StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(id)).getPlaceSound());
    		loc.getWorld().playSound(loc, s, 1.0f, 0.8f);
    		return true;
    	}
		return false;
    }

    /**
     * Play the block place particles for this custom block.
     * @param bukkitBlock the location of which block you want to play place particles of.
     * @return true if the effect was played successfully because the bukkitBlock is a custom block, otherwise false.
     */
    public static boolean playPlaceSound(Block bukkitBlock)
    {
    	return CustomBlock.byAlreadyPlaced(bukkitBlock).playPlaceSound();
    }

    /**
     * Returns whether removing the CustomBlock at the provided location was successful or not.
     * <br>A removal is successful if the Block at the provided location is an actual CustomBlock.
     *
     * @param location The location to remove the CustomBlock from.
     * @return true if the CustomBlock could be removed, otherwise false.
     */
    public static boolean remove(Location location)
    {
    	return CustomBlock.byAlreadyPlaced(location.getBlock()).remove();
    }
    /**
     * Gets the base BlockData for this CustomBlock.
     * It doesn't get the current placed block BlockData but the BlockData used to show the custom block in-game.
     * <p>
     * Warning: TILE CustomBlocks (Spawners) will return null. For now there is no way to obtain this data with this API.
     *
     * @return the base BlockData for this CustomBlock.
     */
    @Nullable
    public BlockData getBaseBlockData()
    {
    	Block block = loc.getBlock();

    	if(NoteBlockMechanicFactory.getInstance().getMechanic(id) != null) {
    		return NoteBlockMechanicFactory.getInstance().createNoteBlockData(id);
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(id) != null) {
            BlockMechanic blockMechanic = (BlockMechanic) BlockMechanicFactory.getInstance().getMechanic(id);
            MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
            
            BlockMechanic.setBlockFacing(newBlockData, blockMechanic.getCustomVariation());
            
            return newBlockData;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(id) != null) {
    		return StringBlockMechanicFactory.getInstance().createTripwireData(id);
    	}
		return null;
    }

    /**
     * Gets the base BlockData for this CustomBlock.
     * It doesn't get the current placed block BlockData but the BlockData used to show the custom block in-game.
     *
     * Warning: TILE CustomBlocks (Spawners) will return null. For now there is no way to obtain this data with this API.
     *
     * @param namespacedID the Namespace and ID in the format {@code namespace:id} of the block to get base BlockData of.
     * @return the base BlockData for this CustomBlock.
     */
    @Nullable
    public static BlockData getBaseBlockData(String namespacedID)
    {
        return CustomBlock.getBaseBlockData(namespacedID);
    }

    /**
     * Returns whether this CustomBlock is placed in the world.
     *
     * @return true if the CustomBlock was placed in the world, otherwise false.
     */
    public boolean isPlaced()
    {
        if(CustomBlock.byAlreadyPlaced(loc.getBlock()).id.equalsIgnoreCase(this.id)) {
        	return true;
        }
        return false;
    }

    /**
     * Returns a list of ItemStack instances to drop as loot for this CustomBlock.
     *
     * @param includeSelfBlock If the CustomBlock itself should be included in the list.
     * @return List containing ItemStack instances for the loot.
     */
    public List<ItemStack> getLoot(boolean includeSelfBlock)
    {
    	
    	List<ItemStack> li = new ArrayList<ItemStack>();
    	
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		NoteBlockMechanic meca = ((NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots().forEach(loot->{
        		if(!includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		
    		
    		return li;
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		BlockMechanic meca = ((BlockMechanic) BlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots().forEach(loot->{
        		if(!includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		return li;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		StringBlockMechanic meca = ((StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots().forEach(loot->{
        		if(!includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		return li;
    	}
		return li;
    }

    /**
     * Returns a list of ItemStack instances to drop as loot for this CustomBlock.
     *
     * @return List containing ItemStack instances for the loot.
     */
    public List<ItemStack> getLoot()
    {
    	Boolean includeSelfBlock = false;
    	
    	List<ItemStack> li = new ArrayList<ItemStack>();
    	
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		NoteBlockMechanic meca = ((NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots().forEach(loot->{
        		if(includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		
    		
    		return li;
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		BlockMechanic meca = ((BlockMechanic) BlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots().forEach(loot->{
        		if(includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		return li;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		StringBlockMechanic meca = ((StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots().forEach(loot->{
        		if(includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		return li;
    	}
		return li;
    }

    /**
     * Returns a list of ItemStack instances to drop as loot for this CustomBlock.
     *
     * @param tool             The Item required to get the loot for the CustomBlock.
     * @param includeSelfBlock If the CustomBlock itself should be included in the list.
     * @return List containing ItemStack instances for the loot.
     */
    public List<ItemStack> getLoot(@Nullable ItemStack tool, boolean includeSelfBlock)
    {
    	
    	List<ItemStack> li = new ArrayList<ItemStack>();
    	
    	if(NoteBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		NoteBlockMechanic meca = ((NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots(tool).forEach(loot->{
        		if(!includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		
    		
    		return li;
    	}
    	else if(BlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		BlockMechanic meca = ((BlockMechanic) BlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots(tool).forEach(loot->{
        		if(!includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		return li;
    	}
    	else if(StringBlockMechanicFactory.getInstance().getMechanic(this.id) != null) {
    		StringBlockMechanic meca = ((StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(id));
    		meca.getDrop().getLoots(tool).forEach(loot->{
        		if(!includeSelfBlock) {
        			if(!loot.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(OraxenItems.getItemById(id).build().getItemMeta().getDisplayName())) {

            			li.add(loot.getItemStack());
        			}
        		}else {
    			li.add(loot.getItemStack());
        		}
    		});
    		return li;
    	}
		return li;
    }


    /**
     * Returns the original light level of this CustomBlock.
     *
     * @return Integer representing the original light level of the CustomBlock.
     */
    @Deprecated
    public int getOriginalLightLevel()
    {
        return 1;
    }

    /**
     * Sets the current light level for this CustomBlock.
     *
     * @param level The light level to set for this CustomBlock.
     */
    @Deprecated
    public void setCurrentLightLevel(int level)
    {
    }
	
}
