package net.md_5.bungee.protocol.packet;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.data.ChatChain;
import net.md_5.bungee.protocol.data.SeenMessages;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ClientCommand extends DefinedPacket
{

    private String command;
    private long timestamp;
    private long salt;
    private Map<String, byte[]> signatures;
    private boolean signedPreview;
    private ChatChain chain;
    private SeenMessages seenMessages;
    private byte checksum;

    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        command = readString( buf, ( protocolVersion >= ProtocolConstants.MINECRAFT_1_20_5 ) ? 32767 : 256 );
        timestamp = buf.readLong();
        salt = buf.readLong();

        int cnt = readVarInt( buf );
        Preconditions.checkArgument( cnt <= 8, "Too many signatures" );
        signatures = new HashMap<>( cnt );
        for ( int i = 0; i < cnt; i++ )
        {
            String name = readString( buf, 16 );
            byte[] signature;

            if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19_3 )
            {
                signature = new byte[ 256 ];
                buf.readBytes( signature );
            } else
            {
                signature = readArray( buf );
            }
            signatures.put( name, signature );
        }

        if ( protocolVersion < ProtocolConstants.MINECRAFT_1_19_3 )
        {
            signedPreview = buf.readBoolean();
        }
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19_3 )
        {
            seenMessages = new SeenMessages();
            seenMessages.read( buf, direction, protocolVersion );
        } else if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19_1 )
        {
            chain = new ChatChain();
            chain.read( buf, direction, protocolVersion );
        }

        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_21_5 )
        {
            checksum = buf.readByte();
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        writeString( command, buf );
        buf.writeLong( timestamp );
        buf.writeLong( salt );

        writeVarInt( signatures.size(), buf );
        for ( Map.Entry<String, byte[]> entry : signatures.entrySet() )
        {
            writeString( entry.getKey(), buf );
            if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19_3 )
            {
                buf.writeBytes( entry.getValue() );
            } else
            {
                writeArray( entry.getValue(), buf );
            }
        }

        if ( protocolVersion < ProtocolConstants.MINECRAFT_1_19_3 )
        {
            buf.writeBoolean( signedPreview );
        }
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19_3 )
        {
            seenMessages.write( buf, direction, protocolVersion );
        } else if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_19_1 )
        {
            chain.write( buf, direction, protocolVersion );
        }

        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_21_5 )
        {
            buf.writeByte( checksum );
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
