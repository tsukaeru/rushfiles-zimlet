package name.w.Zimbra.RushFilesZimlet.Tests;

import name.w.Zimbra.RushFilesZimlet.RushFiles.NetworkDriver;
import name.w.Zimbra.RushFilesZimlet.RushFiles.APIException;
import org.json.JSONObject;

public class NetworkDriverMock extends NetworkDriver
{
    public String request( final String url, final RequestMethod method, final String domainToken, final JSONObject json )
        throws APIException
    {
        return "";
    }
}
