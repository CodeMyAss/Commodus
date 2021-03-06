/*
 * This file is part of Commodus.
 *
 * Commodus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Commodus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Commodus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.commodus.config;

import com.captainbern.reflection.Reflection;
import com.captainbern.reflection.SafeField;
import com.dsh105.commodus.StringUtil;
import org.apache.commons.lang.WordUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;

import static com.captainbern.reflection.matcher.Matchers.withType;

public class Option<T> {

    private FileConfiguration config;
    private String path;
    private T defaultValue;
    private String[] comments;

    public Option(FileConfiguration configuration, String path, String... comments) {
        this.config = configuration;
        this.path = path;

        ArrayList<String> commentsList = new ArrayList<>();
        for (String comment : comments) {
            Collections.addAll(commentsList, WordUtils.wrap(comment, 30, "\n", false).split("\n"));
        }
        this.comments = commentsList.toArray(new String[commentsList.size()]);
    }

    public Option(FileConfiguration configuration, String path, T defaultValue, String... comments) {
        this(configuration, path, comments);
        this.defaultValue = defaultValue;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPath() {
        return path;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public String[] getComments() {
        return comments;
    }

    public String getPath(String... replacements) {
        String path = String.format(getPath(), replacements);
        while (path.endsWith(".")) {
            path = path.substring(0, path.length() - 2);
        }
        return path;
    }

    public T getValue(Options options, Object... replacements) {
        return getValue(options, null, replacements);
    }

    public T getValue(Options options, T defaultValue, Object... replacements) {
        if (options != null && options.isLocked(this, replacements)) {
            return options.getLockedValue(this);
        }
        return getValue(options == null ? null : options.getConfig().config(), defaultValue, StringUtil.convert(replacements));
    }

    public T getValue(FileConfiguration configuration, String... replacements) {
        return getValue(configuration, null, replacements);
    }

    public T getValue(FileConfiguration configuration, T defaultValue, String... replacements) {
        String path = getPath(replacements);
        if (path.contains("%s")) {
            throw new IllegalArgumentException("Not enough path arguments provided");
        }
        if (configuration != null) {
            Object result = configuration.get(getPath(replacements));
            if (result != null) {
                try {
                    return (T) result;
                } catch (ClassCastException ignored) {
                }
            }
        }
        if (getDefaultValue() == null) {
            return defaultValue;
        }
        return getDefaultValue();
    }

    public void setValue(Options options, T value, Object... replacements) {
        setValue(options.getConfig(), value, StringUtil.convert(replacements));
    }

    public void setValue(YAMLConfig yamlConfig, T value, Object... replacements) {
        setValue(yamlConfig.config(), value, StringUtil.convert(replacements));
        yamlConfig.saveConfig();
    }

    public void setValue(FileConfiguration configuration, T value, String... replacements) {
        configuration.set(getPath(replacements), value);
    }

    public static ArrayList<Option> getOptions(Class<?> optionsClass) {
        return getOptions(optionsClass, Option.class);
    }

    public static <T extends Option> ArrayList<T> getOptions(Class<?> optionsClass, Class<T> classRestriction) {
        ArrayList<T> options = new ArrayList<>();
        for (SafeField safeField : new Reflection().reflect(optionsClass).getSafeFields(withType(classRestriction))) {
            Option option = ((SafeField<Option>) safeField).getAccessor().getStatic();
            options.add((T) option);
        }
        return options;
    }
}