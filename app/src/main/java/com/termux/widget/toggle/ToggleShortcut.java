package com.termux.widget.toggle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class ToggleShortcut {

    public final String id;
    public String label;
    public String onScriptPath;
    public String offScriptPath;
    public String iconId;
    public boolean isOn;
    public long createdAt;

    public ToggleShortcut(@NonNull String id,
                          @NonNull String label,
                          @NonNull String onScriptPath,
                          @NonNull String offScriptPath,
                          @Nullable String iconId,
                          boolean isOn) {
        this.id = id;
        this.label = label;
        this.onScriptPath = onScriptPath;
        this.offScriptPath = offScriptPath;
        this.iconId = iconId != null ? iconId : "lucide_power";
        this.isOn = isOn;
        this.createdAt = System.currentTimeMillis();
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("label", label);
            obj.put("onScriptPath", onScriptPath);
            obj.put("offScriptPath", offScriptPath);
            obj.put("iconId", iconId);
            obj.put("isOn", isOn);
            obj.put("createdAt", createdAt);
        } catch (JSONException ignored) {}
        return obj;
    }

    @Nullable
    public static ToggleShortcut fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        try {
            String id = obj.getString("id");
            String label = obj.getString("label");
            String onScript = obj.getString("onScriptPath");
            String offScript = obj.getString("offScriptPath");
            String iconId = obj.optString("iconId", "lucide_power");
            boolean isOn = obj.optBoolean("isOn", false);
            ToggleShortcut toggle = new ToggleShortcut(id, label, onScript, offScript, iconId, isOn);
            toggle.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            return toggle;
        } catch (JSONException e) {
            return null;
        }
    }
}
