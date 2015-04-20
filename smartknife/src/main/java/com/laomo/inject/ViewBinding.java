package com.laomo.inject;

final class ViewBinding {
    private int id;
    private String name;
    private String type;
    private boolean required;
    private boolean click;
    private boolean itemClick;

    public ViewBinding(int id, String name, String type, boolean click, boolean itemClick, boolean required) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.required = required;
        this.click = click;
        this.itemClick = itemClick;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isClick() {
        return click;
    }

    public void setClick(boolean click) {
        this.click = click;
    }

    public boolean isItemClick() {
        return itemClick;
    }

    public void setItemClick(boolean itemClick) {
        this.itemClick = itemClick;
    }
}
