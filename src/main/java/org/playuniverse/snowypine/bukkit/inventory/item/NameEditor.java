package org.playuniverse.snowypine.bukkit.inventory.item;

public class NameEditor extends ColorEditor<NameEditor> {

	public NameEditor(ItemEditor editor) {
		super(editor);
	}

	@Override
	void onInit() {
		if (!editor.hasName())
			return;
		set(editor.getName().get());
	}

	@Override
	public ItemEditor apply() {
		if (editor.hasItemMeta())
			editor.getItemMeta().setDisplayName(content.asColoredString());
		return editor;
	}

}
