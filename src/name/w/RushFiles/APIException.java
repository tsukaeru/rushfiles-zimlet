package name.w.RushFiles;

public class APIException extends Exception
{
    public APIException( Throwable e )
    {
        super( e );
    }

    public APIException( final String message )
    {
        super( message );
    }
}
