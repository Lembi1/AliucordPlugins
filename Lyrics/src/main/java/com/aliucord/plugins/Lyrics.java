package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.utils.ReflectUtils;
import com.discord.api.activity.Activity;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.api.message.embed.MessageEmbed;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.models.presence.Presence;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreUserPresence;
import com.discord.utilities.presence.PresenceUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Lyrics extends Plugin {

    private static final String baseUrl = "https://lyrics-api.powercord.dev/lyrics?input=";
    private static final int MAX_MESSAGE_LENGTH = 2000;

    @NonNull
    @Override
    public Manifest getManifest() {
        Manifest manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Xinto",423915768191647755L) };
        manifest.description = "Get lyrics to a specific song.";
        manifest.version = "1.2.1";
        manifest.updateUrl = "https://raw.githubusercontent.com/X1nto/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context context) {
        ApplicationCommandOption songNameArg = new ApplicationCommandOption(ApplicationCommandType.STRING, "name", "The song name to search lyrics for", null, false, false, null, null);
        ApplicationCommandOption shouldSendArg = new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "send", "To send output in the chat or not", null, false, false, null, null);
        List<ApplicationCommandOption> arguments = Arrays.asList(songNameArg, shouldSendArg);

        commands.registerCommand("lyrics", "Grab a song lyrics", arguments, ctx -> {
            Boolean shouldSend = ctx.getBool("send");
            String songName = ctx.getString("name");

            if (shouldSend == null) {
                shouldSend = false;
            }

            if (songName == null) {
                try {
                    StoreUserPresence userPresence = (StoreUserPresence) StoreStream.class.getMethod("getPresences").invoke(null);
                    Map<Long, Presence> presences = (Map<Long, Presence>) ReflectUtils.getField(userPresence, "presencesSnapshot", true);
                    Presence presence = presences.get(StoreStream.getUsers().getMe().getId());
                    Activity spotifyActivity = PresenceUtils.INSTANCE.getSpotifyListeningActivity(presence);
                    if (spotifyActivity == null) {
                        return new CommandsAPI.CommandResult("You're not listening to anything right now!", null, false);
                    }
                    songName = String.format("%s %s", spotifyActivity.l(), spotifyActivity.e());
                } catch (Exception e) {
                    e.printStackTrace();
                    return new CommandsAPI.CommandResult("Failed to get current Spotify activity.", null, false);
                }
            }

            try {
                ResponseModel.Data data = fetch(songName);
                return shouldSend ? lyricsText(data) : lyricsEmbed(data);
            } catch (Exception e) {
                e.printStackTrace();
                return new CommandsAPI.CommandResult("Failed to fetch data", null, false);
            }
        });
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }

    private CommandsAPI.CommandResult lyricsText(ResponseModel.Data data) {
        String lyrics = data.lyrics;

        if (lyrics.length() > MAX_MESSAGE_LENGTH) {
            String fullLyricsText = String.format("\n\nFull Lyrics: %s", data.url);
            lyrics = lyrics.substring(0, MAX_MESSAGE_LENGTH - fullLyricsText.length() - 3) + "..." + fullLyricsText;
        }

        return new CommandsAPI.CommandResult(lyrics);
    }

    private CommandsAPI.CommandResult lyricsEmbed(ResponseModel.Data data) {
        MessageEmbed embed = new MessageEmbedBuilder()
                .setAuthor(data.artist, null, null)
                .setTitle(data.name)
                .setDescription(data.lyrics)
                .setUrl(data.url)
                .setColor(0x209CEE)
                .setFooter(String.format("Lyrics provided by KSoft.Si | © %s %s", data.artist, data.album_year.split(",")[0]), "https://external-content.duckduckgo.com/iu/?u=https://cdn.ksoft.si/images/Logo128.png")
                .build();

        return new CommandsAPI.CommandResult(null, Collections.singletonList(embed), false, "Lyrics", data.album_art);
    }

    private ResponseModel.Data fetch(String song) throws Exception {
        ResponseModel responseModel = Http.simpleJsonGet(baseUrl + song, ResponseModel.class);
        return responseModel.data.get(0);
    }

    @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"})
    public static class ResponseModel {

        private List<Data> data;

        private static class Data {
            private String lyrics;
            private String artist;
            private String album_year;
            private String album_art;
            private String name;
            private String url;
        }

    }

}