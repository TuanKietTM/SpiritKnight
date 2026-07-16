package com.soulknight.map.json;
//(vitdung) đọc dữ liệu của tầng dữ liệu map

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LayerData {
    public String name;
    public String type;
    public List<Integer> data; // lưu mảng số nếu dữ liệu là map mảng 2 chiều
    public List<ObjectData> objects; //lưu các dữ liệu phân chia phòng (objects)
}
