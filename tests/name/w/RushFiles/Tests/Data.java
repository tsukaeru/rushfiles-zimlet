package name.w.RushFiles.Tests;

import name.w.RushFiles.API;
import name.w.RushFiles.APIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder( MethodOrderer.Alphanumeric.class )
public class Data
{
    protected static final String username = "hopster1222@gmail.com";
    protected static final String password = "L!-M/BBfol";

    protected static String primaryDomain;
    protected static String domainToken;
    protected static API.Share[] shares;
    protected static API.VirtualFile[] shareContent;

    @BeforeAll
    public static void setup() throws APIException
    {
        primaryDomain = API.getPrimaryDomain( username );

        final String deviceId = API.registerDevice(
            primaryDomain,
            username,
            password,
            "device",
            "windows",
            0
        );
        domainToken = API.generateDomainToken( API.getPrimaryDomain( username ), username, password, deviceId, 0, 0 );

        shares = API.getShares( primaryDomain, domainToken, username );
        shareContent = API.getShareContent( primaryDomain, domainToken, shares[ 0 ].Id );
    }

    protected static boolean isPrimaryDomain( final String subject )
    {
        return subject.equals( primaryDomain );
    }

    protected static boolean isDomainToken( final String subject )
    {
        return subject.matches( "^.*Token.*?KeyId.*?Timestamp.*?Thumbprint.*$" );
    }

    protected static boolean isUsername( final String subject )
    {
        return subject.equals( username );
    }

    protected static boolean isPublicLink( final String subject )
    {
        return subject.matches( "https://.*?/client/publiclink.aspx\\?id=.*" );
    }
}
