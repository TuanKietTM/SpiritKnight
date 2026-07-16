package com.soulknight.map.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyData {
    public String name;
    public String type;
    public String value;
}
