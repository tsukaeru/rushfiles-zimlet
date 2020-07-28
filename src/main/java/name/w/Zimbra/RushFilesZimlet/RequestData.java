package name.w.Zimbra.RushFilesZimlet;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

public class RequestData
{
    public final String route;
    public final String cookiePrimaryDomain;
    public final String cookieDomainToken;
    public final String cookieUsername;
    public final String requestUsername;
    public final String requestPassword;
    public final String requestShareId;
    public final String requestInternalName;
    public final JSONArray requestObjects;

    public RequestData( final HttpServletRequest request ) throws IOException
    {
        route = request.getRequestURI().replaceFirst( "/service/extension/.*?/", "" );
        cookiePrimaryDomain = getCookieByName( "primary_domain", request.getCookies() );
        cookieDomainToken = getCookieByName( "domain_token", request.getCookies() );
        cookieUsername = getCookieByName( "username", request.getCookies() );

        final String body = request.getReader().lines().collect( Collectors.joining( System.lineSeparator() ) );
        if( body.isEmpty() ) {
            requestUsername = null;
            requestPassword = null;
            requestShareId = null;
            requestInternalName = null;
            requestObjects = null;
        }
        else {
            final JSONObject requestJson = new JSONObject( body );
            requestUsername = ( requestJson.has( "username" ) ) ? requestJson.getString( "username" ) : null;
            requestPassword = ( requestJson.has( "password" ) ) ? requestJson.getString( "password" ) : null;
            requestShareId = ( requestJson.has( "ShareId" ) ) ? requestJson.getString( "ShareId" ) : null;
            requestInternalName = ( requestJson.has( "InternalName" ) ) ? requestJson.getString( "InternalName" ) : null;
            requestObjects = ( requestJson.has( "objects" ) ) ? requestJson.getJSONArray( "objects" ) : null;
        }
    }

    private String getCookieByName( final String name, final Cookie[] cookies )
    {
        if( cookies == null ) {
            return null;
        }
        for( final Cookie cookie : cookies ) {
            if( name.equals( cookie.getName() ) ) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
