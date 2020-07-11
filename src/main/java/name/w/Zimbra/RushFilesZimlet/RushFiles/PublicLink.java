package name.w.Zimbra.RushFilesZimlet.RushFiles;

public class PublicLink
{
    public final String Id;
    public final long ExpirationTimeUtc;

    private PublicLink( final String id, final long expirationTimeUtc )
    {
        this.Id = id;
        this.ExpirationTimeUtc = expirationTimeUtc;
    }
}