package org.argentumforge.engine.utils.inits;

public final class NpcData {

    private int number;
    private String name = "";
    private int head;
    private int body;
    private int heading;

    public NpcData() {
    }

    public NpcData(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public int getBody() {
        return body;
    }

    public void setBody(int body) {
        this.body = body;
    }

    public int getHeading() { return heading; }

    public void setHeading(int heading) { this.heading = heading; }
}
