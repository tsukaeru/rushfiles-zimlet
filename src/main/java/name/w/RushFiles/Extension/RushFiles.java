package name.w.RushFiles.Extension;

import com.zimbra.cs.extension.ExtensionHttpHandler;
import name.w.RushFiles.Controller;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class RushFiles extends ExtensionHttpHandler
{
    public String getPath()
    {
        return "/rushfiles";
    }

    /**
     * Processes HTTP GET requests
     */
    public void doGet( HttpServletRequest req, HttpServletResponse resp ) throws IOException
    {
        resp.setHeader( "Content-Type", "text/html;charset=UTF-8" );
        resp.getOutputStream().print( "hello" );
    }

    /**
     * Processes HTTP POST requests
     */
    public void doPost( HttpServletRequest req, HttpServletResponse resp ) throws IOException
    {
        resp.setHeader( "Content-Type", "text/json;charset=UTF-8" );

        try {
            final String url = req.getRequestURI().replaceFirst( "/service/extension" + getPath() + "/", "" );
            final String body = req.getReader().lines().collect( Collectors.joining( System.lineSeparator() ) );

            final JSONObject json = ( isJSONValid( body ) ? new JSONObject( body ) : null );
            final Cookie primaryDomainCookie = getCookieByName( "primary_domain", req.getCookies() );
            final Cookie domainTokenCookie = getCookieByName( "domain_token", req.getCookies() );
            final Cookie usernameCookie = getCookieByName( "username", req.getCookies() );

            final var controller = new Controller(
                url,
                json,
                ( primaryDomainCookie == null ) ? null : primaryDomainCookie.getValue(),
                ( domainTokenCookie == null ) ? null : domainTokenCookie.getValue(),
                ( usernameCookie == null ) ? null : usernameCookie.getValue()
            );
            resp.getOutputStream().print( controller.getResponse().toString() );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    private boolean isJSONValid( final String json )
    {
        try {
            new JSONObject( json );
        }
        catch( JSONException ex ) {
            try {
                new JSONArray( json );
            }
            catch( JSONException ex1 ) {
                return false;
            }
        }
        return true;
    }

    private Cookie getCookieByName( final String name, final Cookie[] cookies )
    {
        if( cookies == null ) {
            return null;
        }
        for( final Cookie cookie : cookies ) {
            if( name.equals( cookie.getName() ) ) {
                return cookie;
            }
        }
        return null;
    }
}
