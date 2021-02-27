package org.playuniverse.snowypine.command.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.command.Command;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.command.MinecraftInfo;
import org.playuniverse.snowypine.command.argument.ArgumentHelper;
import org.playuniverse.snowypine.command.argument.ModuleHelper;
import org.playuniverse.snowypine.command.permission.SnowypinePermissions;
import org.playuniverse.snowypine.helper.URLHelper;
import org.playuniverse.snowypine.language.Language;
import org.playuniverse.snowypine.language.LanguageProvider;
import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.language.Variable;
import org.playuniverse.snowypine.module.ModuleExceptionHelper;
import org.playuniverse.snowypine.utils.general.Placeholder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.net.http.Request;
import com.syntaxphoenix.syntaxapi.net.http.RequestType;
import com.syntaxphoenix.syntaxapi.net.http.Response;
import com.syntaxphoenix.syntaxapi.net.http.StandardContentType;
import com.syntaxphoenix.syntaxapi.utils.java.Arrays;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ModuleCommand extends Command {

	public static final Command COMMAND = new ModuleCommand();

	public static CommandInfo info() {
		return new CommandInfo("module", COMMAND, "plugin").setUsage("{<module> / <page>}").setDescription("Module management command")
			.setDetailedDescription(
				"/module list - List all modules (hover for more info)", 
				"/module enable <module> - Enables a module",
				"/module disable <module> - Disables a module", 
				"/module download <url> - Downloads a module",
				"/module update <module> - Updates a module (if it specified an update url)", 
				"/module delete <module> - Deletes a module",
				"/module reload {<module>} - Reloads all modules or just the specified one", 
				"/module info <module> - Shows the information of the module",
				"/module load - Loads new modules")
			.setPermission(SnowypinePermissions.COMMAND_MODULE);
	}

	private ModuleCommand() {}

	@Override
	public void execute(MinecraftInfo info, Arguments arguments) {

		String command = ArgumentHelper.get(arguments, 1, ArgumentType.STRING).map(argument -> argument.asString().getValue()).orElse("");

		switch (command) {
		case "list":
			List<PluginWrapper> wrappers = Snowypine.getPlugin().getPluginManager().getPlugins();
			List<ChatColor> colors = wrappers.stream().map(wrapper -> ModuleHelper.colorByState(wrapper.getPluginState())).collect(Collectors.toList());

			Collections.sort(wrappers, (c1, c2) -> c1.getPluginId().compareTo(c2.getPluginId()));

			int amount = wrappers.size();
			long enabled = colors.stream().filter(color -> color == ChatColor.GREEN).count();
			long resolved = colors.stream().filter(color -> color == ChatColor.YELLOW).count();
			long disabled = colors.stream().filter(color -> color == ChatColor.RED).count();
			long failed = colors.stream().filter(color -> color == ChatColor.DARK_RED).count();

			Language language = LanguageProvider.getLanguageOf(info.getSender());
			BaseComponent[] message = TextComponent.fromLegacyText(Message.COMMAND_MODULE_LIST_START.legacyMessage(language, Placeholder.of("enabled", enabled),
				Placeholder.of("resolved", resolved), Placeholder.of("disabled", disabled), Placeholder.of("failed", failed)), ChatColor.WHITE);
			BaseComponent[] split = TextComponent.fromLegacyText(Variable.COMMAND_MODULE_LIST_SPLIT.legacyMessage(language), ChatColor.WHITE);
			Message item = Message.COMMAND_MODULE_LIST_ITEM;
			Message hover = Message.COMMAND_MODULE_LIST_HOVER;

			for (int index = 0; index < amount; index++) {
				PluginWrapper wrapper = wrappers.get(index);
				PluginDescriptor descriptor = wrapper.getDescriptor();

				BaseComponent[] components = TextComponent.fromLegacyText(item.legacyMessage(language,
					Placeholder.of("color", ModuleHelper.colorByState(wrapper.getPluginState())), Placeholder.of("name", wrapper.getPluginId())),
					ChatColor.WHITE);
				HoverEvent event = new HoverEvent(Action.SHOW_TEXT,
					new Text(
						hover.legacyMessage(language, Placeholder.of("plugin", descriptor.getPluginId()), Placeholder.of("author", descriptor.getProvider()),
							Placeholder.of("version", descriptor.getVersion()), Placeholder.of("description", descriptor.getPluginDescription()),
							Placeholder.of("color", ModuleHelper.colorByState(wrapper.getPluginState())), Placeholder.of("state", wrapper.getPluginState()))));
				for (BaseComponent component : components) {
					component.setHoverEvent(event);
				}
				if (index + 1 != amount) {
					components = Arrays.merge(BaseComponent[]::new, components, split);
				}
				message = Arrays.merge(BaseComponent[]::new, message, components);
			}
			info.getSender().spigot().sendMessage(message);
			return;
		case "reload":
			if (arguments.count() >= 2) {
				Optional<PluginWrapper> option = ArgumentHelper.get(arguments, 2, argument -> ModuleHelper.getModule(argument));
				if (!option.isPresent()) {
					Message.COMMAND_MODULE_DOESNT_EXIST.send(info.getSender(),
						Placeholder.of("name", ArgumentHelper.get(arguments, 2).map(argument -> argument.asObject().toString()).orElse("N/A")));
					return;
				}
				Message.WORK_IN_PROGRESS.send(info.getSender());
			} else {
				Message.COMMAND_MODULE_RELOAD_ALL_START.send(info.getSender());
				PluginManager pluginManager = Snowypine.getPlugin().getPluginManager();
				pluginManager.stopPlugins();
				pluginManager.unloadPlugins();
				Snowypine.getPlugin().loadPlugins();
				int started = pluginManager.getStartedPlugins().size();
				int total = pluginManager.getPlugins().size();
				if (started != total) {
					Message.COMMAND_MODULE_WENT_WRONG.send(info.getSender());
				}
				Message.COMMAND_MODULE_RELOAD_ALL_END.send(info.getSender(), Placeholder.of("amount", started), Placeholder.of("total", total));
			}
			return;
		case "load":
			Message.WORK_IN_PROGRESS.send(info.getSender());
			return;
		case "info":
			if (arguments.count() == 1) {
				info.getInfo().sendSimple(info.getSender(), info.getLabel());
				return;
			}
			Optional<PluginWrapper> option = ArgumentHelper.get(arguments, 2, argument -> ModuleHelper.getModule(argument));
			if (!option.isPresent()) {
				Message.COMMAND_MODULE_DOESNT_EXIST.send(info.getSender(),
					Placeholder.of("name", ArgumentHelper.get(arguments, 2).map(argument -> argument.asObject().toString()).orElse("N/A")));
				return;
			}
			PluginWrapper wrapper = option.get();
			PluginDescriptor descriptor = wrapper.getDescriptor();
			Message.COMMAND_MODULE_LIST_HOVER.send(info.getSender(), Placeholder.of("plugin", descriptor.getPluginId()),
				Placeholder.of("author", descriptor.getProvider()), Placeholder.of("version", descriptor.getVersion()),
				Placeholder.of("description", descriptor.getPluginDescription()), Placeholder.of("color", ModuleHelper.colorByState(wrapper.getPluginState())),
				Placeholder.of("state", wrapper.getPluginState()));
			return;
		case "enable":
			if (arguments.count() == 1) {
				info.getInfo().sendSimple(info.getSender(), info.getLabel());
				return;
			}
			Optional<PluginWrapper> option0 = ArgumentHelper.get(arguments, 2, argument -> ModuleHelper.getModule(argument));
			if (!option0.isPresent()) {
				Message.COMMAND_MODULE_DOESNT_EXIST.send(info.getSender(),
					Placeholder.of("name", ArgumentHelper.get(arguments, 2).map(argument -> argument.asObject().toString()).orElse("N/A")));
				return;
			}
			PluginWrapper wrapper0 = option0.get();
			if (wrapper0.getPluginState() == PluginState.STARTED) {
				Message.COMMAND_MODULE_HAS_ALREADY.send(info.getSender(), Placeholder.of("color", ModuleHelper.colorByState(wrapper0.getPluginState())),
					Placeholder.of("state", wrapper0.getPluginState()));
				return;
			}
			Message.COMMAND_MODULE_ENABLE_START.send(info.getSender(), Placeholder.of("name", wrapper0.getPluginId()));
			try {
				wrapper0.getPluginManager().startPlugin(wrapper0.getPluginId());
				Message.COMMAND_MODULE_ENABLE_END.send(info.getSender());
			} catch (PluginRuntimeException exp) {
				ModuleExceptionHelper.log(wrapper0, exp, "Failed to enable Plugin");
				Message.COMMAND_MODULE_WENT_WRONG.send(info.getSender());
			}
			return;
		case "disable":
			if (arguments.count() == 1) {
				info.getInfo().sendSimple(info.getSender(), info.getLabel());
				return;
			}
			Optional<PluginWrapper> option1 = ArgumentHelper.get(arguments, 2, argument -> ModuleHelper.getModule(argument));
			if (!option1.isPresent()) {
				Message.COMMAND_MODULE_DOESNT_EXIST.send(info.getSender(),
					Placeholder.of("name", ArgumentHelper.get(arguments, 2).map(argument -> argument.asObject().toString()).orElse("N/A")));
				return;
			}
			PluginWrapper wrapper1 = option1.get();
			if (wrapper1.getPluginState() != PluginState.STARTED) {
				Message.COMMAND_MODULE_HAS_ALREADY.send(info.getSender(), Placeholder.of("color", ModuleHelper.colorByState(wrapper1.getPluginState())),
					Placeholder.of("state", wrapper1.getPluginState()));
				return;
			}
			Message.COMMAND_MODULE_DISABLE_START.send(info.getSender(), Placeholder.of("name", wrapper1.getPluginId()));
			try {
				wrapper1.getPluginManager().stopPlugin(wrapper1.getPluginId());
				Message.COMMAND_MODULE_DISABLE_END.send(info.getSender());
			} catch (PluginRuntimeException exp) {
				ModuleExceptionHelper.log(wrapper1, exp, "Failed to disable Plugin");
				Message.COMMAND_MODULE_WENT_WRONG.send(info.getSender());
			}
			return;
		case "download":
			if (arguments.count() == 1) {
				info.getInfo().sendSimple(info.getSender(), info.getLabel());
				return;
			}
			String url = ArgumentHelper.get(arguments, 2, ArgumentType.STRING).map(argument -> argument.asString().getValue()).orElse("");
			if (!URLHelper.isUrl(url)) {
				Message.COMMAND_MODULE_DOWNLOAD_INVALID.send(info.getSender(), Placeholder.of("input", url));
				return;
			}
			Message.COMMAND_MODULE_DOWNLOAD_START.send(info.getSender(), Placeholder.of("url", url));
			Request request = new Request(RequestType.GET);
			try {
				Response response = request.execute(url, StandardContentType.URL_ENCODED);
				if (response.getCode() != 200) {
					Message.COMMAND_MODULE_DOWNLOAD_NON222EXISTENT.send(info.getSender(), Placeholder.of("url", url));
					return;
				}
				String fileName = null;
				if (response.get("Content-Disposition") == null) {
					if (url.endsWith("/")) {
						url = url.substring(0, url.length() - 1);
					}
					if (!url.endsWith(".jar")) {
						Message.COMMAND_MODULE_DOWNLOAD_NON222EXISTENT.send(info.getSender(), Placeholder.of("url", url));
						return;
					}
					String[] parts = url.split("\\/");
					fileName = parts[parts.length - 1];
				}
				if (fileName == null) {
					JsonArray array = response.getMultiple("Content-Disposition");
					for (JsonElement element : array) {
						if (!element.isJsonPrimitive()) {
							continue;
						}
						String current = element.getAsString();
						if (!current.startsWith("filename=")) {
							continue;
						}
						fileName = (fileName = current.split("=")[1]).substring(1, fileName.length() - 1);
					}
				}
				if (fileName == null || !fileName.endsWith(".jar")) {
					Message.COMMAND_MODULE_DOWNLOAD_NON222EXISTENT.send(info.getSender(), Placeholder.of("url", url));
					return;
				}
				File file = new File(Snowypine.getPlugin().getPluginDirectory(), fileName);
				if (file.exists()) {
					Message.COMMAND_MODULE_DOWNLOAD_ALREADY222EXISTENT.send(info.getSender(), Placeholder.of("name", fileName));
					return;
				}
				file.createNewFile();
				FileOutputStream stream = new FileOutputStream(file);
				stream.write(response.getResponseBytes());
				stream.flush();
				stream.close();
				Message.COMMAND_MODULE_DOWNLOAD_END.send(info.getSender(), Placeholder.of("name", fileName));
			} catch (Throwable throwable) {
				Message.COMMAND_MODULE_DOWNLOAD_NON222EXISTENT.send(info.getSender(), Placeholder.of("url", url));
				return;
			}

			return;
		case "update":
			if (arguments.count() == 1) {
				info.getInfo().sendSimple(info.getSender(), info.getLabel());
				return;
			}
			Optional<PluginWrapper> option2 = ArgumentHelper.get(arguments, 2, argument -> ModuleHelper.getModule(argument));
			if (!option2.isPresent()) {
				Message.COMMAND_MODULE_DOESNT_EXIST.send(info.getSender(),
					Placeholder.of("name", ArgumentHelper.get(arguments, 2).map(argument -> argument.asObject().toString()).orElse("N/A")));
				return;
			}
			Message.WORK_IN_PROGRESS.send(info.getSender());
			return;
		case "delete":
			if (arguments.count() == 1) {
				info.getInfo().sendSimple(info.getSender(), info.getLabel());
				return;
			}
			Optional<PluginWrapper> option3 = ArgumentHelper.get(arguments, 2, argument -> ModuleHelper.getModule(argument));
			if (!option3.isPresent()) {
				Message.COMMAND_MODULE_DOESNT_EXIST.send(info.getSender(),
					Placeholder.of("name", ArgumentHelper.get(arguments, 2).map(argument -> argument.asObject().toString()).orElse("N/A")));
				return;
			}
			PluginWrapper wrapper3 = option3.get();
			Message.COMMAND_MODULE_DELETE_START.send(info.getSender(), Placeholder.of("name", wrapper3.getPluginId()));
			try {
				wrapper3.getPluginManager().deletePlugin(wrapper3.getPluginId());
				Message.COMMAND_MODULE_DELETE_END.send(info.getSender());
			} catch (PluginRuntimeException exp) {
				ModuleExceptionHelper.log(wrapper3, exp, "Failed to delete Plugin");
				Message.COMMAND_MODULE_WENT_WRONG.send(info.getSender());
			}
			return;
		default:
			info.getInfo().sendSimple(info.getSender(), info.getLabel());
			return;
		}

	}

	@Override
	public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
		return new DefaultCompletion();
	}

}