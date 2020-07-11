package name.w.Zimbra.RushFilesZimlet.RushFiles;

import org.json.JSONException;
import org.json.JSONObject;

public class Share
{
    public final String Id;
    public final String CompanyId;
    public final String Name;

    private Share( final String aId, final String aCompanyId, final String aName )
    {
        Id = aId;
        CompanyId = aCompanyId;
        Name = aName;
    }

    public JSONObject toJson()
    {
        try {
            return new JSONObject()
                .put( "Id", Id )
                .put( "CompanyId", CompanyId )
                .put( "Name", Name );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }
}