package name.w.Zimbra.RushFilesZimlet.RushFiles;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class NetworkDriver
{
    protected String requestGET( final String url, final String domainToken ) throws APIException
    {
        return request( url, RequestMethod.GET, domainToken, null );
    }

    protected String requestPOST( final String url, final String domainToken, final JSONObject json )
        throws APIException
    {
        return request( url, RequestMethod.POST, domainToken, json );
    }

    protected String requestPUT( final String url, final String domainToken, final JSONObject json ) throws APIException
    {
        return request( url, RequestMethod.PUT, domainToken, json );
    }

    protected String request( final String url, final RequestMethod method, final String domainToken, final JSONObject json )
        throws APIException
    {
        try {
            final var con = ( HttpURLConnection ) new URL( url ).openConnection();
            con.setDoOutput( true );

            con.setRequestMethod( method.name() );
            con.setRequestProperty( "Content-Type", "application/json" );
            if( domainToken != null ) {
                con.setRequestProperty( "Authorization", "DomainToken " + domainToken );
            }

            if( json != null ) {
                final OutputStream os = con.getOutputStream();
                os.write( json
                    .toString()
                    .getBytes( StandardCharsets.UTF_8 ) );
                os.close();
            }

            final InputStream in;
            if( con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED ) {
                throw new APIException( "unauthorized" );
            }
            else if( con.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST ) {
                throw new APIException( "bad request" );
            }
            else if( con.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ||
                     con.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN ) {
                in = new BufferedInputStream( con.getErrorStream() );
            }
            else {
                in = new BufferedInputStream( con.getInputStream() );
            }
            final BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );

            final StringBuilder result = new StringBuilder();
            String line;
            while( ( line = reader.readLine() ) != null ) {
                result.append( line );
            }

            return String.valueOf( result );
        }
        catch( UnknownHostException e ) {
            throw new APIException( "unknown host: " + e.getMessage() );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public enum RequestMethod
    {
        GET, POST, PUT
    }
}
