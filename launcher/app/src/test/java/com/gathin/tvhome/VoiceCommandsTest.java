package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceCommandsTest {
    @Test public void understandsOnlyCompleteSingleCommands() {
        assertEquals(VoiceCommand.YOUTUBE, VoiceCommands.parse("Открой  ютуб!"));
        assertEquals(VoiceCommand.VOLUME_DOWN, VoiceCommands.parse("сделай тише"));
        assertEquals(VoiceCommand.HDMI_2, VoiceCommands.parse("аш ди эм ай два"));
        assertEquals(VoiceCommand.NONE, VoiceCommands.parse("не открывай ютуб"));
        assertEquals(VoiceCommand.NONE, VoiceCommands.parse("открой ютуб и выключи звук"));
        assertEquals(VoiceCommand.NONE, VoiceCommands.parse("нажмите окей затем удерживайте микрофон"));
        assertEquals(VoiceCommand.NONE, VoiceCommands.parse("[unk] открой ютуб"));
        assertEquals(VoiceCommand.NONE, VoiceCommands.parse(null));
    }
    @Test public void rejectsUncertainAndEmptyRecognition() {
        assertEquals(VoiceCommand.NONE, VoiceCommands.accept("открой ютуб", .79, .9));
        assertEquals(VoiceCommand.NONE, VoiceCommands.accept("открой ютуб", .95, .4));
        assertEquals(VoiceCommand.YOUTUBE, VoiceCommands.accept("открой ютуб", .95, .9));
        assertEquals(VoiceCommand.NONE, VoiceCommands.accept("", 1, 1));
    }
}
