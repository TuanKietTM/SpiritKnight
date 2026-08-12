//(vitdung) class đọc dữ liệu từ json
//class này đọc thông sóo cơ bản nhất của map
package com.soulknight.map.json;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class TiledMapData {
        public int width;
        public int height;
        public int tilewidth;
        public List<LayerData> layers;
}
