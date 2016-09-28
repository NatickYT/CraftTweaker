/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package minetweaker.runtime.providers;

import minetweaker.MineTweakerAPI;
import minetweaker.runtime.IScriptIterator;
import minetweaker.runtime.IScriptProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Stan
 */
public class ScriptProviderDirectory implements IScriptProvider {
    private final File directory;

    public ScriptProviderDirectory(File directory) {
        if (directory == null)
            throw new IllegalArgumentException("directory cannot be null");

        this.directory = directory;
    }

    @Override
    public Iterator<IScriptIterator> getScripts() {
        List<IScriptIterator> scripts = new ArrayList<IScriptIterator>();
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    scripts.add(new ScriptIteratorDirectory(file));
                } else if (file.getName().endsWith(".zs")) {
                    scripts.add(new ScriptIteratorSingle(file));
                } else if (file.getName().endsWith(".zip")) {
                    try {
                        scripts.add(new ScriptIteratorZip(file));
                    } catch (IOException ex) {
                        MineTweakerAPI.logError("Could not load " + file.getName() + ": " + ex.getMessage());
                    }
                }
            }
        }
        scripts.sort(new Comparator<IScriptIterator>() {
            @Override
            public int compare(IScriptIterator sc, IScriptIterator sc1) {
                return sc.getName().compareTo(sc1.getName());
            }
        });
        return scripts.iterator();
    }
}
