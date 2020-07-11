package name.w.Zimbra.RushFilesZimlet.RushFiles;

public class Authenticator
{
    private NetworkDriver network = new NetworkDriver();

    public void redefineNetworkDriver( final NetworkDriver driver )
    {
        network = driver;
    }

    public API unauthorized( final String username, final String password ) throws APIException
    {
        return new API( username, password, network );
    }

    public API authorized( final String primaryDomain, final String domainToken, final String username )
    {
        return new API( primaryDomain, domainToken, username, network );
    }
}
