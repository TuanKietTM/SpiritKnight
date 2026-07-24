package com.soulknight.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;

public class StoryConfigLoader {

    public String bootScript;
    public String javaCode;
    public String consoleScript;
    public String signalScript;
    public String corruptedCode;
    public List<StoryFrameData> frames;

    public static class StoryFrameData {
        public String imagePath;
        public String title;
        public String text;
        public double delay;

        public StoryFrame toStoryFrame() {
            return new StoryFrame(imagePath, title, text, delay);
        }
    }

    public static StoryConfigLoader loadFromJson(String resourcePath) {
        try (InputStream is = StoryConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Khong tim thay file jon cot truyen  " + resourcePath);
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, StoryConfigLoader.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Loi khi doc file cot truyen  " + e.getMessage());
        }
    }
}