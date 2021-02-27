package org.playuniverse.snowypine.bukkit.inventory.item;

public class LoreEditor extends ColorEditor<LoreEditor> {

	public LoreEditor(ItemEditor editor) {
		super(editor);
	}

	@Override
	void onInit() {
		if (!editor.hasLore())
			return;
		set(editor.getLore().get());
	}

	@Override
	public ItemEditor apply() {
		if (editor.hasItemMeta())
			editor.getItemMeta().setLore(content.asColoredList());
		return editor;
	}

}