package name.w.RushFiles;

public class ControllerException extends Exception
{
    public ControllerException( Throwable e )
    {
        super( e );
    }

    public ControllerException( final String msg )
    {
        super( msg );
    }
}