package com.deadmole.copytothebox.util;

public class SyncTarget {
    public final String sourcePath;
    public final String moduleName;

    public SyncTarget(String sourcePath, String moduleName) {
        this.sourcePath = sourcePath;
        this.moduleName = moduleName;
    }
}
