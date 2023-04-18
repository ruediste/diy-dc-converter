package com.github.ruediste;

import java.io.Serializable;

import javax.inject.Singleton;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.Atomic.Var;

@Singleton
public class Persistence {
    private DB db;
    public Var<String> currentMode;
    public HTreeMap<String, Serializable> settings;
    public Var<Boolean> autoSend;

    @SuppressWarnings("unchecked")
    public void initialize() {
        db = DBMaker.fileDB("control.db").transactionEnable().make();
        currentMode = db.atomicVar("currentMode", Serializer.STRING).createOrOpen();
        autoSend = db.atomicVar("autoSend", Serializer.BOOLEAN).createOrOpen();
        settings = db.hashMap("settings", Serializer.STRING, Serializer.JAVA).createOrOpen();
    }

    public void commit() {
        this.db.commit();
    }
}
