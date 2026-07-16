package com.soulknight.map.json;
//(vitdung) class này lưu dữ liệu của các object (là vị trí của phòng, cửa, spawn...)
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectData {
    public String name;
    public String type;
    public double x;
    public double y;
    public double width;
    public double height;
    public List<PropertyData> properties;

    //hàm lấy nhanh value từ tên của property
    public String getPropertyValue(String proName) {
        if (properties == null) return null;
        for (PropertyData p : properties) {
            if (p.name.equals(proName)) return p.value;
        }
        return null;
    }
}
