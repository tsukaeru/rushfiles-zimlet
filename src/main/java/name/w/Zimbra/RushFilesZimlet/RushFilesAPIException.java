package name.w.Zimbra.RushFilesZimlet;

public class RushFilesAPIException extends Exception
{
    public RushFilesAPIException( Throwable e )
    {
        super( e );
    }

    public RushFilesAPIException( final String message )
    {
        super( message );
    }
}
