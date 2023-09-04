package com.lx862.btellraw.data;

public class TellrawEntry {
    public String fileName;
    public String content;
    public String fullID;
    public String ID;

    public TellrawEntry(String fileName, String content, String fullID, String ID) {
        this.fileName = fileName;
        this.content = content;
        this.fullID = fullID;
        this.ID = ID;
    }
}
