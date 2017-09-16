package com.josesamuel.logviewer.view;

import com.android.utils.Pair;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.josesamuel.logviewer.log.LogProcess;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Renderer used for the process list view
 */
public class ClientCellRenderer extends ColoredListCellRenderer {
    @NotNull
    private final String myEmptyText;

    ClientCellRenderer() {
        this("");
    }

    private ClientCellRenderer(@NotNull String emptyText) {
        this.myEmptyText = emptyText;
    }

    /**
     * Render the given {@link LogProcess} object
     */
    private void renderClient(@NotNull LogProcess c, ColoredTextContainer container) {
        String name = c.getProcessName();
        if (name != null) {
            Pair<String, String> app = splitApplicationName(name);
            container.append(app.getFirst(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            container.append(app.getSecond(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

            if (c.getProcessID() != 0) {
                container.append(String.format(" (%1$d)", c.getProcessID()), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        }
    }

    /**
     * Split the name if possible as package and app name
     */
    private Pair<String, String> splitApplicationName(String name) {
        int index = name.lastIndexOf('.');
        if (index != -1) {
            return Pair.of(name.substring(0, index + 1), name.substring(index + 1));
        } else {
            return Pair.of("", name);
        }
    }

    /**
     * Gets called to render an item
     */
    protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof LogProcess) {
            renderClient((LogProcess) value, this);
        } else if (value == null) {
            this.append(this.myEmptyText, SimpleTextAttributes.ERROR_ATTRIBUTES);
        } else {
            this.append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

    }

}
