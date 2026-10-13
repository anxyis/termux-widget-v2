package com.termux.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class TermuxCustomCommandPreferences {

    private static final String PREF_NAME = "termux_custom_commands";
    private static final String KEY_COMMANDS_JSON = "commands_list";

    public static class CustomCommand {
        public String id;
        public String name;
        public String command;
        public boolean isTask;
        public String iconId;
        public String group;

        public CustomCommand(String id, String name, String command, boolean isTask, String iconId, String group) {
            this.id = id;
            this.name = name;
            this.command = command;
            this.isTask = isTask;
            this.iconId = iconId != null && !iconId.isEmpty() ? iconId : "lucide_terminal";
            this.group = group != null && !group.isEmpty() ? group : "Commands";
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("command", command);
            obj.put("isTask", isTask);
            obj.put("iconId", iconId);
            obj.put("group", group);
            return obj;
        }

        public static CustomCommand fromJson(JSONObject obj) {
            return new CustomCommand(
                    obj.optString("id"),
                    obj.optString("name"),
                    obj.optString("command"),
                    obj.optBoolean("isTask", false),
                    obj.optString("iconId", "lucide_terminal"),
                    obj.optString("group", "Commands")
            );
        }
    }

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static List<CustomCommand> getCustomCommands(@NonNull Context context) {
        List<CustomCommand> list = new ArrayList<>();
        String jsonStr = getPrefs(context).getString(KEY_COMMANDS_JSON, null);
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(CustomCommand.fromJson(arr.getJSONObject(i)));
                }
            } catch (JSONException ignored) {}
        }
        return list;
    }

    @Nullable
    public static CustomCommand getCommandById(@NonNull Context context, @NonNull String id) {
        for (CustomCommand c : getCustomCommands(context)) {
            if (c.id.equals(id)) return c;
        }
        return null;
    }

    public static void saveCustomCommand(@NonNull Context context, @NonNull CustomCommand cmd) {
        List<CustomCommand> existing = getCustomCommands(context);
        boolean updated = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).id.equals(cmd.id)) {
                existing.set(i, cmd);
                updated = true;
                break;
            }
        }
        if (!updated) {
            existing.add(cmd);
        }
        saveAllCommands(context, existing);
    }

    public static void deleteCustomCommand(@NonNull Context context, @NonNull String cmdId) {
        List<CustomCommand> existing = getCustomCommands(context);
        List<CustomCommand> remaining = new ArrayList<>();
        for (CustomCommand c : existing) {
            if (!c.id.equals(cmdId)) remaining.add(c);
        }
        saveAllCommands(context, remaining);
    }

    private static void saveAllCommands(@NonNull Context context, List<CustomCommand> list) {
        JSONArray arr = new JSONArray();
        for (CustomCommand c : list) {
            try {
                arr.put(c.toJson());
            } catch (JSONException ignored) {}
        }
        getPrefs(context).edit().putString(KEY_COMMANDS_JSON, arr.toString()).apply();
    }
}
