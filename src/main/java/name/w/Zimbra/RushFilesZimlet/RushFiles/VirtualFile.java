package name.w.Zimbra.RushFilesZimlet.RushFiles;

import org.json.JSONException;
import org.json.JSONObject;

public class VirtualFile
{
    public final boolean IsFile;
    public final String InternalName;
    public final String PublicName;
    public final String ShareId;

    private VirtualFile( final boolean aIsFile, final String aInternalName, final String aPublicName,
                         final String aShareId )
    {
        IsFile = aIsFile;
        InternalName = aInternalName;
        PublicName = aPublicName;
        ShareId = aShareId;
    }

    public JSONObject toJson()
    {
        try {
            return new JSONObject()
                .put( "IsFile", IsFile )
                .put( "InternalName", InternalName )
                .put( "PublicName", PublicName )
                .put( "ShareId", ShareId );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }
}