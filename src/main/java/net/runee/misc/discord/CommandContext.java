package net.runee.misc.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.runee.errors.CommandException;
import net.runee.errors.GuildContextRequiredException;
import net.runee.errors.InsufficientPermissionsException;
import net.runee.misc.Utils;
import net.runee.misc.logging.Logger;

public class CommandContext {
    private static final Logger logger = new Logger(CommandContext.class);

    public CommandContext(JDA jda, User author, MessageChannel replyChannel) {
        this(jda, author, replyChannel, null);
    }

    public CommandContext(JDA jda, User author, MessageChannel replyChannel, Guild guild) {
        this.jda = jda;
        this.author = author;
        this.replyChannel = replyChannel;
        this.guild = guild;
    }

    public CommandContext(MessageReceivedEvent e) {
        this(e.getJDA(), e.getAuthor(), e.getChannel(), e.isFromGuild() ? e.getGuild() : null);
    }

    // core
    private JDA jda;
    private User author;
    private MessageChannel replyChannel;
    private Guild guild;
    // convenience
    private Command command;

    // core
    public JDA getJDA() {
        return jda;
    }

    public User getAuthor() {
        return author;
    }

    public MessageChannel getReplyChannel() {
        return replyChannel;
    }

    public void setReplyChannel(MessageChannel replyChannel) {
        this.replyChannel = replyChannel;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public Guild getGuild() {
        return guild;
    }

    public void run(Command command, String... args) {
        this.command = command;
        try {
            command.execute(this, args);
        } catch (CommandException ex) {
            reply(new EmbedBuilder()
                    .setDescription(ex.getReplyMessage())
                    .setColor(Utils.colorRed)
                    .build()
            );
        } catch (Exception ex) {
            logger.error("Failed to execute command '" + command.getName() + " " + String.join(" ", args) + "'", ex);
            replyWarning("Failed to execute command, please take a look at the console for details!");
        }
        this.command = null;
    }

    // convenience
    public void reply(MessageEmbed embed) {
        replyChannel.sendMessage(embed).queue();
    }

    public void reply(Message msg) {
        replyChannel.sendMessage(msg).queue();
    }

    public void reply(CharSequence text) {
        replyChannel.sendMessage(text).queue();
    }

    public void replyWarning(CharSequence text) {
        reply(new EmbedBuilder()
                .setDescription(text)
                .setColor(Utils.colorRed)
                .build()
        );
    }

    public void replySuccess(CharSequence text) {
        reply(new EmbedBuilder()
                .setDescription(text)
                .setColor(Utils.colorGreen)
                .build()
        );
    }

    public boolean isGuildContext() {
        return guild != null;
    }

    public Guild ensureGuildContext() throws GuildContextRequiredException {
        if (guild == null) {
            throw new GuildContextRequiredException(command, this);
        }
        return guild;
    }

    public Guild ensureAdminPermission() throws InsufficientPermissionsException, GuildContextRequiredException {
        ensureGuildContext();
        Member author = guild.getMember(this.author);
        if(author == null) {
            throw new RuntimeException("Failed to get your guild member info");
        }
        if(!author.hasPermission(Permission.ADMINISTRATOR) && !this.isAppOwner()) {
            throw new InsufficientPermissionsException(command, this);
        }
        return guild;
    }

    public void ensureOwnerPermission() throws InsufficientPermissionsException {
        ApplicationInfo appInfo = jda.retrieveApplicationInfo().complete();
        if(!author.equals(appInfo.getOwner())) {
            throw new InsufficientPermissionsException(command, this);
        }
    }

    public boolean isAppOwner() {
        ApplicationInfo appInfo = jda.retrieveApplicationInfo().complete();
        return author.equals(appInfo.getOwner());
    }
}
