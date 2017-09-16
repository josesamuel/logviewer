package com.josesamuel.logviewer.log.dnd;

import com.intellij.ide.dnd.*;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

/**
 * Sets up drag and drop handling
 */
public class DnDHandler {


    private DnDTargetChecker dnDTargetChecker = event -> {
        if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            event.setDropPossible(true);
            return true;
        }
        return false;
    };
    private DnDListener dnDListener;
    private DnDDropHandler dropHandler = new DnDDropHandler() {
        @Override
        public void drop(DnDEvent event) {
            event.setDropPossible(true);
            try {
                DnDNativeTarget.EventInfo eventInfo = (DnDNativeTarget.EventInfo) event.getTransferData(DataFlavor.javaFileListFlavor);
                Transferable transferable = eventInfo.getTransferable();
                List data = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for (Object aData : data) {
                    dnDListener.onFileDropped((File) aData);
                    break;
                }
            } catch (Exception ignored) {
            }
        }
    };

    public DnDHandler(DnDListener dnDListener) {
        this.dnDListener = dnDListener;
    }

    /**
     * Adds the DND support for the editor components
     */
    public void addDndSupportForComponent(JComponent component) {
        addDndSupport(component);
        int size = component.getComponentCount();
        for (int i = 0; i < size; i++) {
            if (component.getComponent(i) instanceof JComponent) {
                addDndSupportForComponent((JComponent) component.getComponent(i));
            }
        }
    }

    /**
     * Add DND support to this component
     */
    private void addDndSupport(JComponent component) {
        DnDSupport.createBuilder(component)
                .setDropHandler(dropHandler)
                .setTargetChecker(dnDTargetChecker)
                .enableAsNativeTarget()
                .install();
    }
}
