package com.org.ollamafx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartCollection {

    public enum Criteria {
        DATE, // e.g. "Today", "Last 7 Days"
        KEYWORD, // e.g. "Project X"
        MODEL // e.g. "llama3"
    }

    private String id;
    private String name;
    private String icon; // Feather icon name
    private Criteria criteria;
    private String value;
    private boolean isExpanded;

    public SmartCollection() {
        this.id = UUID.randomUUID().toString();
        this.isExpanded = true;
    }

    public SmartCollection(String name, Criteria criteria, String value, String icon) {
        this();
        this.name = name;
        this.criteria = criteria;
        this.value = value;
        this.icon = icon;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
